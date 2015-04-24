package org.ambraproject.rhino.service.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.AssetNodesByDoi;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.model.DoiAssociation;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.view.internal.RepoVersionRepr;
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
import java.util.LinkedHashMap;
import java.util.Map;

class VersionedIngestionService {

  private static final Logger log = LoggerFactory.getLogger(VersionedIngestionService.class);

  private final ArticleCrudServiceImpl parentService;

  VersionedIngestionService(ArticleCrudServiceImpl parentService) {
    this.parentService = Preconditions.checkNotNull(parentService);
  }

  public RepoCollectionMetadata ingest(Archive archive) throws IOException, XmlContentException {
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
    ArticleXml parsedArticle;
    try (InputStream manuscriptStream = new BufferedInputStream(archive.openFile(manuscriptEntry))) {
      parsedArticle = new ArticleXml(AmbraService.parseXml(manuscriptStream));
    }
    ArticleIdentity articleIdentity = parsedArticle.readDoi();

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

    AssetNodesByDoi assetNodeMap = parsedArticle.findAllAssetNodes();

    // Create RepoObjects for assets
    AssetTable<String> assetTable = AssetTable.buildFromIngestible(assetNodeMap, manifestXml);
    for (AssetTable.Asset<String> asset : assetTable.getAssets()) {
      AssetIdentity assetIdentity = asset.getIdentity();
      String key = asset.getFileType() + "/" + assetIdentity.getIdentifier();
      AssetFileIdentity assetFileIdentity = AssetFileIdentity.create(assetIdentity.getIdentifier(), asset.getReprName());
      String archiveEntryName = asset.getFileLocator();
      RepoObject repoObject = new RepoObject.RepoObjectBuilder(key)
          .contentAccessor(archive.getContentAccessorFor(archiveEntryName))
          .userMetadata(createUserMetadataForArchiveEntryName(archiveEntryName))
          .downloadName(assetFileIdentity.getFileName())
          .contentType(assetFileIdentity.inferContentType().toString())
          .build();
      toUpload.put(archiveEntryName, repoObject);
    }

    // Create RepoObjects for files in the archive not referenced by the manifest
    int nonAssetFileIndex = 0;
    for (String entry : archive.getEntryNames()) {
      if (!toUpload.containsKey(entry)) {
        String key = "nonAssetFile-" + (++nonAssetFileIndex) + "/" + articleIdentity.getIdentifier();
        RepoObject repoObject = new RepoObject.RepoObjectBuilder(key)
            .contentAccessor(archive.getContentAccessorFor(entry))
            .userMetadata(createUserMetadataForArchiveEntryName(entry))
            .downloadName(entry)
            .contentType(AssetFileIdentity.parse(entry).inferContentType().toString())
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

    Map<String, Object> userMetadataForCollection = buildArticleAsUserMetadata(manifestXml, created, assetTable);

    // Create collection
    RepoCollection collection = RepoCollection.builder()
        .setKey(articleIdentity.getIdentifier())
        .setObjects(created.values())
        .setUserMetadata(parentService.crepoGson.toJson(userMetadataForCollection))
        .build();
    RepoCollectionMetadata collectionMetadata = parentService.contentRepoService.autoCreateCollection(collection);

    // Associate DOIs
    for (AssetIdentity assetIdentity : assetTable.getAssetIdentities()) {
      String assetDoi = assetIdentity.getIdentifier();
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

    return collectionMetadata;
  }


  private String createUserMetadataForArchiveEntryName(String entryName) {
    ImmutableMap<String, String> map = ImmutableMap.of(ArticleCrudServiceImpl.ARCHIVE_ENTRY_NAME_KEY, entryName);
    return parentService.crepoGson.toJson(map);
  }

  private Map<String, Object> buildArticleAsUserMetadata(ManifestXml manifestXml,
                                                         Map<String, RepoVersion> objects,
                                                         AssetTable<String> assetTable) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("format", "nlm");

    String manuscriptKey = manifestXml.getArticleXml();
    map.put("manuscript", new RepoVersionRepr(objects.get(manuscriptKey)));

    map.put("assets", assetTable.buildAsAssetMetadata(objects));

    return map;
  }

}
