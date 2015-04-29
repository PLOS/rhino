package org.ambraproject.rhino.service.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.model.DoiAssociation;
import org.ambraproject.rhino.rest.RestClientException;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    Article articleMetadata;
    try (InputStream manuscriptStream = new BufferedInputStream(archive.openFile(manuscriptEntry))) {
      ArticleXml parsedArticle = new ArticleXml(AmbraService.parseXml(manuscriptStream));
      articleIdentity = parsedArticle.readDoi();
      articleMetadata = parsedArticle.build(new Article());
    }

    ArticleCollection collection = new ArticleCollection(manifestXml, articleIdentity);

    ArticleObject manifest = collection.insertArchiveObject(manifestEntry,
        new RepoObject.RepoObjectBuilder("manifest/" + articleIdentity.getIdentifier())
            .contentAccessor(archive.getContentAccessorFor(manifestEntry))
            .downloadName(manifestEntry)
            .contentType(MediaType.APPLICATION_XML));
    collection.tagSpecialObject("manifest", manifest);

    ArticleObject manuscript = collection.insertArchiveObject(manuscriptEntry,
        new RepoObject.RepoObjectBuilder("manuscript/" + articleIdentity.getIdentifier())
            .contentAccessor(archive.getContentAccessorFor(manuscriptEntry))
            .contentType(MediaType.APPLICATION_XML)
            .downloadName(articleIdentity.forXmlAsset().getFileName()));
    collection.tagSpecialObject("manuscript", manuscript);

    for (ManifestXml.Asset asset : assets) {
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        String entry = representation.getEntry();
        if (entry.equals(manuscriptEntry)) continue;
        if (!archive.getEntryNames().contains(entry)) {
          // TODO: Rollback needed?
          throw new RestClientException("Manifest refers to missing file: " + entry, HttpStatus.BAD_REQUEST);
        }
        String key = representation.getName() + "/" + AssetIdentity.create(asset.getUri()).getIdentifier();

        collection.insertArchiveObject(entry,
            new RepoObject.RepoObjectBuilder(key)
                .contentAccessor(archive.getContentAccessorFor(entry))
                    // TODO Add more metadata. Extract from articleMetadata and manifestXml as necessary.
                .contentType(MediaType.APPLICATION_OCTET_STREAM) // Temporary! TODO: Remove
        );
      }
    }

    // Create RepoObjects for files in the archive not referenced by the manifest
    int nonAssetFileIndex = 0;
    for (String entry : archive.getEntryNames()) {
      if (!collection.archiveObjects.containsKey(entry)) {
        String key = "nonAssetFile-" + (++nonAssetFileIndex) + "/" + articleIdentity.getIdentifier();
        RepoObject.RepoObjectBuilder repoObject = new RepoObject.RepoObjectBuilder(key)
            .contentAccessor(archive.getContentAccessorFor(entry))
            .contentType(MediaType.APPLICATION_OCTET_STREAM) // Temporary! TODO: Infer from file name?
            ;
        collection.insertArchiveObject(entry, repoObject);
      }
    }

    RepoCollectionMetadata collectionMetadata = collection.persist();

    // Associate DOIs
    for (ManifestXml.Asset asset : manifestXml.parse()) {
      String assetDoi = AssetIdentity.create(asset.getUri()).getIdentifier();
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

  private static class RepoVersionRepr {
    private final String key;
    private final String uuid;

    private RepoVersionRepr(RepoVersion repoVersion) {
      this.key = repoVersion.getKey();
      this.uuid = repoVersion.getUuid().toString();
    }
  }


  private static class ArticleObject {

    /**
     * Object to be created.
     */
    private final RepoObject input;

    /**
     * Name of the archive entry from the ingestible representing this object. Absent if the object was not originally
     * part of the archive, but was dynamically generated.
     */
    private final Optional<String> archiveEntryName;

    /**
     * The result from persisting the object. Null if it has not been persisted yet; set when it is persisted.
     */
    private RepoObjectMetadata created;

    private ArticleObject(RepoObject input, Optional<String> archiveEntryName) {
      this.input = Preconditions.checkNotNull(input);
      this.archiveEntryName = Preconditions.checkNotNull(archiveEntryName);
    }
  }

  private ArticleObject createDynamicObject(RepoObject repoObject) {
    return new ArticleObject(repoObject, Optional.<String>absent());
  }

  private class ArticleCollection {
    private final Map<String, ArticleObject> archiveObjects = new LinkedHashMap<>(); // keys are archiveEntryNames
    private final Map<String, ArticleObject> specialObjects = new LinkedHashMap<>(); // keys to be used in JSON

    private final ManifestXml manifestXml;
    private final ArticleIdentity articleIdentity;

    private ArticleCollection(ManifestXml manifestXml, ArticleIdentity articleIdentity) {
      this.manifestXml = Preconditions.checkNotNull(manifestXml);
      this.articleIdentity = Preconditions.checkNotNull(articleIdentity);
    }

    public ArticleObject insertArchiveObject(String entryName, RepoObject.RepoObjectBuilder builder) {
      ImmutableMap<String, String> userMetadata = ImmutableMap.of(ArticleCrudServiceImpl.ARCHIVE_ENTRY_NAME_KEY, entryName);
      builder.userMetadata(parentService.crepoGson.toJson(userMetadata));

      ArticleObject articleObject = new ArticleObject(builder.build(), Optional.of(entryName));
      archiveObjects.put(articleObject.archiveEntryName.get(), articleObject);
      return articleObject;
    }

    public void tagSpecialObject(String name, ArticleObject articleObject) {
      specialObjects.put(name, articleObject);
    }

    private Collection<ArticleObject> getAllObjects() {
      Set<ArticleObject> allObjects = new LinkedHashSet<>(archiveObjects.size() + specialObjects.size());
      allObjects.addAll(archiveObjects.values());
      allObjects.addAll(specialObjects.values());
      return allObjects;
    }

    public RepoCollectionMetadata persist() {
      // Persist objects
      Collection<ArticleObject> allObjects = getAllObjects();
      Collection<RepoVersion> createdObjects = new ArrayList<>(allObjects.size());
      for (ArticleObject articleObject : allObjects) { // Excellent candidate for parallelization! I can haz JDK8 plz?
        articleObject.created = parentService.contentRepoService.autoCreateRepoObject(articleObject.input);
        createdObjects.add(articleObject.created.getVersion());
      }

      Map<String, Object> userMetadata = buildUserMetadata();

      // Persist collection
      RepoCollection collection = RepoCollection.builder()
          .setKey(articleIdentity.getIdentifier())
          .setObjects(createdObjects)
          .setUserMetadata(parentService.crepoGson.toJson(userMetadata))
          .build();
      return parentService.contentRepoService.autoCreateCollection(collection);
    }

    private RepoVersion getVersionForCreatedEntry(String archiveEntryName) {
      return archiveObjects.get(archiveEntryName).created.getVersion();
    }

    private Map<String, Object> buildUserMetadata() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("format", "nlm");

      for (Map.Entry<String, ArticleObject> entry : specialObjects.entrySet()) {
        RepoVersion version = entry.getValue().created.getVersion();
        map.put(entry.getKey(), new RepoVersionRepr(version));
      }

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
          RepoVersion objectForRepr = getVersionForCreatedEntry(representation.getEntry());
          assetObjects.put(representation.getName(), new RepoVersionRepr(objectForRepr));
        }
        assetMetadata.put("objects", assetObjects);

        assetList.add(assetMetadata);
      }
      map.put("assets", assetList);

      return map;
    }
  }

}
