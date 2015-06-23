package org.ambraproject.rhino.service.impl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.model.DoiAssociation;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService.ArticleMetadataSource;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.view.internal.RepoVersionRepr;
import org.plos.crepo.model.RepoCollection;
import org.plos.crepo.model.RepoCollectionList;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.model.RepoVersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

class VersionedIngestionService {

  private static final Logger log = LoggerFactory.getLogger(VersionedIngestionService.class);

  private final ArticleCrudServiceImpl parentService;

  VersionedIngestionService(ArticleCrudServiceImpl parentService) {
    this.parentService = Preconditions.checkNotNull(parentService);
  }

  static class IngestionResult {
    private final Article article;
    private final RepoCollectionList collection;

    public IngestionResult(Article article, RepoCollectionList collection) {
      this.article = Preconditions.checkNotNull(article);
      this.collection = Preconditions.checkNotNull(collection);
    }

    public Article getArticle() {
      return article;
    }

    public RepoCollectionList getCollection() {
      return collection;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      IngestionResult that = (IngestionResult) o;
      return article.equals(that.article) && collection.equals(that.collection);
    }

    @Override
    public int hashCode() {
      return 31 * article.hashCode() + collection.hashCode();
    }
  }

  IngestionResult ingest(Archive archive) throws IOException, XmlContentException {
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
    final Article articleMetadata = parsedArticle.build(new Article());

    AssetTable<String> assetTable = AssetTable.buildFromIngestible(parsedArticle.findAllAssetNodes(), manifestXml);
    ArticleCollection collection = new ArticleCollection(archive.getArchiveName(), articleIdentity, assetTable);

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
    collection.tagSpecialObject(SOURCE_KEYS.get(ArticleMetadataSource.FULL_MANUSCRIPT), manuscript);

    collection.tagSpecialObject(SOURCE_KEYS.get(ArticleMetadataSource.FRONT_MATTER), createDynamicObject(
        new RepoObject.RepoObjectBuilder("front/" + articleIdentity.getIdentifier())
            .byteContent(serializeXml(parsedArticle.extractFrontMatter()))
            .contentType(MediaType.APPLICATION_XML)
            .build()));
    collection.tagSpecialObject(SOURCE_KEYS.get(ArticleMetadataSource.FRONT_AND_BACK_MATTER), createDynamicObject(
        new RepoObject.RepoObjectBuilder("frontAndBack/" + articleIdentity.getIdentifier())
            .byteContent(serializeXml(parsedArticle.extractFrontAndBackMatter()))
            .contentType(MediaType.APPLICATION_XML)
            .build()));

    // Create RepoObjects for assets
    for (AssetTable.Asset<String> asset : assetTable.getAssets()) {
      AssetIdentity assetIdentity = asset.getIdentity();
      String key = asset.getFileType() + "/" + assetIdentity.getIdentifier();
      AssetFileIdentity assetFileIdentity = buildAssetFileIdentity(manifestXml, asset);
      String archiveEntryName = asset.getFileLocator();

      ArticleObject preexisting = collection.archiveObjects.get(archiveEntryName);
      if (preexisting != null) {
        if (!preexisting.input.getKey().equals(key)) {
          String message = String.format("Mismatched objects (%s, %s) with same archiveEntryName: %s",
              preexisting.input.getKey(), key, archiveEntryName);
          throw new RuntimeException(message);
        }
        continue; // Don't insert a RepoObject redundant to a special object created above
      }

      RepoObject.RepoObjectBuilder repoObject = new RepoObject.RepoObjectBuilder(key)
          .contentAccessor(archive.getContentAccessorFor(archiveEntryName))
          .userMetadata(createUserMetadataForArchiveEntryName(archiveEntryName))
          .downloadName(assetFileIdentity.getFileName())
          .contentType(assetFileIdentity.inferContentType().toString());
      collection.insertArchiveObject(archiveEntryName, repoObject);
    }

    // Create RepoObjects for files in the archive not referenced by the manifest
    int nonAssetFileIndex = 0;
    for (String entry : archive.getEntryNames()) {
      if (!collection.archiveObjects.containsKey(entry)) {
        String key = "nonAssetFile-" + (++nonAssetFileIndex) + "/" + articleIdentity.getIdentifier();
        RepoObject.RepoObjectBuilder repoObject = new RepoObject.RepoObjectBuilder(key)
            .contentAccessor(archive.getContentAccessorFor(entry))
            .userMetadata(createUserMetadataForArchiveEntryName(entry))
            .downloadName(entry)
            .contentType(AssetFileIdentity.parse(entry).inferContentType().toString());
        collection.insertArchiveObject(entry, repoObject);
      }
    }

    RepoCollectionList collectionMetadata = collection.persist();

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

    return new IngestionResult(articleMetadata, collectionMetadata);
  }

  private static byte[] serializeXml(Document document) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.transform(new DOMSource(document), new StreamResult(outputStream));
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return outputStream.toByteArray();
  }

  /*
   * Build an AssetFileIdentity by looking up the file extension from the manifest. This information isn't retained in
   * the AssetTable.Asset object but we need it temporarily to build the filename.
   *
   * Implementation note: Calling this once for each asset causes quadratic performance costs. This is probably not a
   * big deal (happens only once per ingestion with everything on the heap). If it is found to be a problem, it could be
   * solved by building a hash map from AssetTable.Asset keys to ManifestXml.Representation values.
   */
  private static AssetFileIdentity buildAssetFileIdentity(ManifestXml manifest, AssetTable.Asset<String> asset) {
    String assetEntry = asset.getFileLocator();
    for (ManifestXml.Asset assetObj : manifest.parse()) {
      for (ManifestXml.Representation representation : assetObj.getRepresentations()) {
        if (representation.getEntry().equals(assetEntry)) {
          return AssetFileIdentity.create(asset.getIdentity().getIdentifier(), representation.getName());
        }
      }
    }
    throw new RuntimeException("Could not match " + assetEntry + " to manifest");
  }


  private static final String ARCHIVE_NAME_KEY = "archiveName";
  private static final String ARCHIVE_ENTRY_NAME_KEY = "archiveEntryName";
  private static final Function<RepoObjectMetadata, String> ARCHIVE_ENTRY_NAME_EXTRACTOR = new Function<RepoObjectMetadata, String>() {
    @Nullable
    @Override
    public String apply(RepoObjectMetadata input) {
      Optional<Object> jsonUserMetadata = input.getJsonUserMetadata();
      if (jsonUserMetadata.isPresent()) {
        Object metadataValue = jsonUserMetadata.get();
        if (metadataValue instanceof Map) {
          return (String) ((Map) metadataValue).get(ARCHIVE_ENTRY_NAME_KEY);
        }
      }
      return null; // default to downloadName value
    }
  };

  private String createUserMetadataForArchiveEntryName(String entryName) {
    ImmutableMap<String, String> map = ImmutableMap.of(ARCHIVE_ENTRY_NAME_KEY, entryName);
    return parentService.crepoGson.toJson(map);
  }

  public Archive repack(RepoCollectionList article) {
    Map<String, Object> articleMetadata = (Map<String, Object>) article.getJsonUserMetadata().get();
    String archiveName = (String) articleMetadata.get(ARCHIVE_NAME_KEY);
    return Archive.readCollection(parentService.contentRepoService, archiveName, article, ARCHIVE_ENTRY_NAME_EXTRACTOR);
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

    private final String archiveName;
    private final ArticleIdentity articleIdentity;
    private final AssetTable<String> assetTable;

    private ArticleCollection(String archiveName, ArticleIdentity articleIdentity, AssetTable<String> assetTable) {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(archiveName));
      this.archiveName = archiveName;
      this.articleIdentity = Preconditions.checkNotNull(articleIdentity);
      this.assetTable = Preconditions.checkNotNull(assetTable);
    }

    public ArticleObject insertArchiveObject(String entryName, RepoObject.RepoObjectBuilder builder) {
      builder.userMetadata(createUserMetadataForArchiveEntryName(entryName));

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

    public RepoCollectionList persist() {
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

    private Map<String, Object> buildUserMetadata() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("format", "nlm");
      map.put(ARCHIVE_NAME_KEY, archiveName);

      for (Map.Entry<String, ArticleObject> entry : specialObjects.entrySet()) {
        RepoVersion version = entry.getValue().created.getVersion();
        map.put(entry.getKey(), new RepoVersionRepr(version));
      }

      Map<String, Object> assetMetadata = assetTable.buildAsAssetMetadata(Maps.transformValues(archiveObjects,
          new Function<ArticleObject, RepoVersion>() {
            @Override
            public RepoVersion apply(ArticleObject articleObject) {
              return articleObject.created.getVersion();
            }
          }));
      map.put("assets", assetMetadata);

      return map;
    }

  }


  private static final ImmutableBiMap<ArticleMetadataSource, String> SOURCE_KEYS = ImmutableBiMap.<ArticleMetadataSource, String>builder()
      .put(ArticleMetadataSource.FULL_MANUSCRIPT, "manuscript")
      .put(ArticleMetadataSource.FRONT_MATTER, "front")
      .put(ArticleMetadataSource.FRONT_AND_BACK_MATTER, "frontAndBack")
      .build();

  static {
    if (!SOURCE_KEYS.keySet().equals(EnumSet.allOf(ArticleMetadataSource.class))) {
      throw new AssertionError("ArticleMetadataSource values don't match key map");
    }
  }

  /**
   * Build a representation of an article's metadata from a persisted collection.
   * <p/>
   * The legacy Hibernate model object {@link Article} is used as a data-holder for convenience and compatibility. This
   * method constructs it anew, not by accessing Hibnerate, and populates only a subset of its normal fields.
   * <p/>
   * The method signature will probably change when the deprecated methods {@link org.ambraproject.rhino.rest.controller.ArticleCrudController#previewMetadataFromVersionedModel}
   * and {@link org.ambraproject.rhino.service.ArticleCrudService#readVersionedMetadata} are removed. At minimum, the
   * {@code source} argument should be removed in favor of always using the "front" file. Also, the means of specifying
   * a collection version (the {@code id} and {@code versionNumber} parameters) might be refactored.
   *
   * @param id            the ID of the article to serve
   * @param versionNumber the number of the ingested version to read, or absent for the latest version
   * @param source        whether to parse the extracted front matter or the full, original manuscript
   * @return an object containing metadata that could be extracted from the manuscript, with other fields unfilled
   */
  @Deprecated
  Article getArticleMetadata(ArticleIdentity id, Optional<Integer> versionNumber, ArticleMetadataSource source) {
    String identifier = id.getIdentifier();
    RepoCollectionMetadata collection;
    if (versionNumber.isPresent()) {
      collection = parentService.contentRepoService.getCollection(new RepoVersionNumber(identifier, versionNumber.get()));
    } else {
      collection = parentService.contentRepoService.getLatestCollection(identifier);
    }

    Map<String, Object> userMetadata = (Map<String, Object>) collection.getJsonUserMetadata().get();
    Map<String, String> manuscriptId = (Map<String, String>) userMetadata.get(SOURCE_KEYS.get(source));
    RepoVersion manuscript = RepoVersionRepr.read(manuscriptId);

    Document document;
    try (InputStream manuscriptStream = parentService.contentRepoService.getRepoObject(manuscript)) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder(); // TODO: Efficiency
      log.debug("In getArticleMetadata source={} documentBuilder.parse() called", source);
      document = documentBuilder.parse(manuscriptStream);
      log.debug("finish");
    } catch (IOException | SAXException | ParserConfigurationException e) {
      throw new RuntimeException(e);
    }

    Article article;
    try {
      article = new ArticleXml(document).build(new Article());
    } catch (XmlContentException e) {
      throw new RuntimeException(e);
    }
    article.setLastModified(collection.getTimestamp());
    return article;
  }

}
