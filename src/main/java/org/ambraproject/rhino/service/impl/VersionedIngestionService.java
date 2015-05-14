package org.ambraproject.rhino.service.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.model.DoiAssociation;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.util.Archive;
import org.plos.crepo.model.RepoCollection;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class VersionedIngestionService {

  private static final Logger log = LoggerFactory.getLogger(VersionedIngestionService.class);

  private final ArticleCrudServiceImpl parentService;

  VersionedIngestionService(ArticleCrudServiceImpl parentService) {
    this.parentService = Preconditions.checkNotNull(parentService);
  }

  ArticleCrudService.IngestionResult ingest(Archive archive) throws IOException, XmlContentException {
    String manifestEntry = null;
    for (String entryName : archive.getEntryNames()) {
      if (entryName.equalsIgnoreCase("manifest.xml")) {
        manifestEntry = entryName;
      }
    }
    if (manifestEntry == null) {
      throw new RestClientException("Archive has no manifest file", HttpStatus.BAD_REQUEST);
    }

    Map<String, RepoObject> toUpload = new LinkedHashMap<>(); // keys are zip entry names

    ManifestXml manifestXml;
    try (InputStream manifestStream = new BufferedInputStream(archive.openFile(manifestEntry))) {
      manifestXml = new ManifestXml(AmbraService.parseXml(manifestStream));
    }
    ImmutableList<ManifestXml.Asset> assets = manifestXml.parse();

    ManifestXml.Asset manuscriptAsset = null;
    ManifestXml.Representation manuscriptRepr = null;
    for (ManifestXml.Asset asset : assets) {
      Optional<String> mainEntry = asset.getMainEntry();
      if (mainEntry.isPresent()) {
        for (ManifestXml.Representation representation : asset.getRepresentations()) {
          if (representation.getEntry().equals(mainEntry.get())) {
            manuscriptRepr = representation;
            break;
          }
        }
        manuscriptAsset = asset;
        break;
      }
    }
    if (manuscriptAsset == null || manuscriptRepr == null) {
      throw new RestClientException("main-entry not found", HttpStatus.BAD_REQUEST);
    }

    String manuscriptEntry = manuscriptRepr.getEntry();
    if (!archive.getEntryNames().contains(manifestEntry)) {
      throw new RestClientException("Manifest refers to missing file as main-entry: " + manuscriptEntry, HttpStatus.BAD_REQUEST);
    }
    ArticleIdentity articleIdentity;
    final Article articleMetadata;
    try (InputStream manuscriptStream = new BufferedInputStream(archive.openFile(manuscriptEntry))) {
      ArticleXml parsedArticle = new ArticleXml(AmbraService.parseXml(manuscriptStream));
      articleIdentity = parsedArticle.readDoi();
      articleMetadata = parsedArticle.build(new Article());
    }

    RepoObject manifestObject = new RepoObject.RepoObjectBuilder("manifest/" + articleIdentity.getIdentifier())
        .contentAccessor(archive.getContentAccessorFor(manifestEntry))
        .downloadName(manifestEntry)
        .contentType(MediaType.APPLICATION_XML)
        .userMetadata(createUserMetadataForArchiveEntryName(manifestEntry))
        .build();
    toUpload.put(manifestEntry, manifestObject);

    RepoObject manuscriptObject = new RepoObject.RepoObjectBuilder("manuscript/" + articleIdentity.getIdentifier())
        .contentAccessor(archive.getContentAccessorFor(manuscriptEntry))
        .contentType(MediaType.APPLICATION_XML)
        .downloadName(articleIdentity.forXmlAsset().getFileName())
        .userMetadata(createUserMetadataForArchiveEntryName(manuscriptEntry))
        .build();
    toUpload.put(manuscriptEntry, manuscriptObject);

    for (ManifestXml.Asset asset : assets) {
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        String entry = representation.getEntry();
        if (entry.equals(manuscriptEntry)) continue;
        if (!archive.getEntryNames().contains(entry)) {
          // TODO: Rollback needed?
          throw new RestClientException("Manifest refers to missing file: " + entry, HttpStatus.BAD_REQUEST);
        }
        String key = representation.getName() + "/" + AssetIdentity.create(asset.getUri()).getIdentifier();
        RepoObject repoObject = new RepoObject.RepoObjectBuilder(key)
            .contentAccessor(archive.getContentAccessorFor(entry))
            .userMetadata(createUserMetadataForArchiveEntryName(entry))
                // TODO Add more metadata. Extract from articleMetadata and manifestXml as necessary.
            .contentType(MediaType.APPLICATION_OCTET_STREAM) // Temporary! TODO: Remove
            .build();
        toUpload.put(entry, repoObject);
      }
    }

    // Create RepoObjects for files in the archive not referenced by the manifest
    int nonAssetFileIndex = 0;
    for (String entry : archive.getEntryNames()) {
      if (!toUpload.containsKey(entry)) {
        String key = "nonAssetFile-" + (++nonAssetFileIndex) + "/" + articleIdentity.getIdentifier();
        RepoObject repoObject = new RepoObject.RepoObjectBuilder(key)
            .contentAccessor(archive.getContentAccessorFor(entry))
            .userMetadata(createUserMetadataForArchiveEntryName(entry))
            .contentType(MediaType.APPLICATION_OCTET_STREAM) // Temporary! TODO: Infer from file name?
            .build();
        toUpload.put(entry, repoObject);
      }
    }

    // Post files
    Map<String, RepoVersion> created = new LinkedHashMap<>();
    for (Map.Entry<String, RepoObject> entry : toUpload.entrySet()) { // Excellent candidate for parallelization! I can haz JDK8 plz?
      RepoObject repoObject = entry.getValue();
      RepoObjectMetadata createdMetadata = parentService.contentRepoService.autoCreateRepoObject(repoObject);
      created.put(entry.getKey(), createdMetadata.getVersion());
    }

    ArticleUserMetadata userMetadataForCollection = buildArticleAsUserMetadata(manifestXml, created);

    // Create collection
    RepoCollection collection = RepoCollection.builder()
        .setKey(articleIdentity.getIdentifier())
        .setObjects(created.values())
        .setUserMetadata(parentService.crepoGson.toJson(userMetadataForCollection.map))
        .build();
    final RepoCollectionMetadata collectionMetadata = parentService.contentRepoService.autoCreateCollection(collection);

    // Associate DOIs
    for (String assetDoi : userMetadataForCollection.dois) {
      assetDoi = AssetIdentity.create(assetDoi).getIdentifier();
      DoiAssociation existing = (DoiAssociation) DataAccessUtils.uniqueResult(parentService.hibernateTemplate.find(
          "from DoiAssociation where doi=?", assetDoi));
      if (existing == null) {
        DoiAssociation association = new DoiAssociation();
        association.setDoi(assetDoi);
        association.setParentArticleDoi(articleIdentity.getIdentifier());
        parentService.hibernateTemplate.persist(association);
      } else if (!existing.getParentArticleDoi().equalsIgnoreCase(articleIdentity.getIdentifier())) {
        throw new RuntimeException("Asset DOI already belongs to another parent article"); // TODO: Rollback
      } // else, leave it as is
    }

    return new ArticleCrudService.IngestionResult(articleMetadata, collectionMetadata);
  }

  private String createUserMetadataForArchiveEntryName(String entryName) {
    ImmutableMap<String, String> map = ImmutableMap.of(ArticleCrudServiceImpl.ARCHIVE_ENTRY_NAME_KEY, entryName);
    return parentService.crepoGson.toJson(map);
  }

  private static class ArticleUserMetadata {
    private final Map<String, Object> map;
    private final Set<String> dois;

    private ArticleUserMetadata(Map<String, Object> map, Collection<String> dois) {
      this.map = ImmutableMap.copyOf(map);
      this.dois = ImmutableSet.copyOf(dois);
    }
  }

  private static class RepoVersionRepr {
    private final String key;
    private final String uuid;

    private RepoVersionRepr(RepoVersion repoVersion) {
      this.key = repoVersion.getKey();
      this.uuid = repoVersion.getUuid().toString();
    }
  }

  private ArticleUserMetadata buildArticleAsUserMetadata(ManifestXml manifestXml, Map<String, RepoVersion> objects) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("format", "nlm");

    String manuscriptKey = manifestXml.getArticleXml();
    map.put("manuscript", new RepoVersionRepr(objects.get(manuscriptKey)));

    List<ManifestXml.Asset> assetSpec = manifestXml.parse();
    List<Map<String, Object>> assetList = new ArrayList<>(assetSpec.size());
    List<String> assetDois = new ArrayList<>(assetSpec.size());
    for (ManifestXml.Asset asset : assetSpec) {
      Map<String, Object> assetMetadata = new LinkedHashMap<>();
      String doi = AssetIdentity.create(asset.getUri()).getIdentifier();
      assetMetadata.put("doi", doi);
      assetDois.add(doi);

      Map<String, Object> assetObjects = new LinkedHashMap<>();
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        RepoVersion objectForRepr = objects.get(representation.getEntry());
        assetObjects.put(representation.getName(), new RepoVersionRepr(objectForRepr));
      }
      assetMetadata.put("objects", assetObjects);

      assetList.add(assetMetadata);
    }
    map.put("assets", assetList);

    return new ArticleUserMetadata(map, assetDois);
  }

}
