package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService.ArticleMetadataSource;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.view.internal.RepoVersionRepr;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.plos.crepo.model.RepoCollection;
import org.plos.crepo.model.RepoCollectionList;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

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

  /**
   * Identifies the model that we are using for representing articles as CRepo collections. To be stored in the
   * collection's {@code userMetadata} field under the "schema" key.
   * <p>
   * Currently, this is not consumed anywhere. The "schema" field is future-proofing against changes to the model that
   * would require backfilling or special handling. The "format" value can be used to disambiguate different models or
   * different inputs. ("ambra-nlm" means that the article is an XML file under the NLM DTD, packaged as a zip file with
   * manifest as input for Ambra.) The "version" value can be incremented to reflect changes in ingestion logic that
   * break backwards compatibility.
   */
  private static final ImmutableMap<String, Object> SCHEMA_REPR = ImmutableMap.<String, Object>builder()
      .put("format", "ambra-nlm")
      .put("version", 1)
      .build();

  Article ingest(Archive archive, OptionalInt revision) throws IOException, XmlContentException {
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

    ManifestXml.Asset manuscriptAsset = findManuscriptAsset(assets);
    ManifestXml.Representation manuscriptRepr = findManuscriptRepr(manuscriptAsset);
    ManifestXml.Representation printableRepr = findPrintableRepr(manuscriptAsset);

    String manuscriptEntry = manuscriptRepr.getEntry();
    if (!archive.getEntryNames().contains(manifestEntry)) {
      throw new RestClientException("Manifest refers to missing file as main-entry: " + manuscriptEntry, HttpStatus.BAD_REQUEST);
    }

    ArticleXml parsedArticle;
    try (InputStream manuscriptStream = new BufferedInputStream(archive.openFile(manuscriptEntry))) {
      parsedArticle = new ArticleXml(AmbraService.parseXml(manuscriptStream));
    }
    ArticleIdentity articleIdentity = parsedArticle.readDoi();
    if (!manuscriptAsset.getUri().equals(articleIdentity.getKey())) {
      String message = String.format("Article DOI is inconsistent. From manifest: \"%s\" From manuscript: \"%s\"",
          manuscriptAsset.getUri(), articleIdentity.getKey());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
    final Article articleMetadata = parsedArticle.build(new Article());
    articleMetadata.setDoi(articleIdentity.getKey());

    ArticlePackage articlePackage = new ArticlePackageBuilder(archive, parsedArticle, manifestXml, manifestEntry, manuscriptRepr, printableRepr).build();
    PersistenceResult result = persist(articlePackage);

    persistRevision(result, revision.orElseGet(parsedArticle::getRevisionNumber));

    stubAssociativeFields(articleMetadata);
    return articleMetadata;
  }

  private static class PersistenceResult {
    private final PersistedWork article;
    private final ImmutableList<PersistedWork> assets;

    private PersistenceResult(PersistedWork article, List<PersistedWork> assets) {
      this.article = Objects.requireNonNull(article);
      this.assets = ImmutableList.copyOf(assets);
    }

    public PersistedWork getArticle() {
      return article;
    }

    public Stream<PersistedWork> getWorks() {
      return Stream.concat(Stream.of(article), assets.stream());
    }
  }

  private static class PersistedWork {
    private final ScholarlyWork scholarlyWork;
    private final RepoCollectionList result;

    private PersistedWork(ScholarlyWork scholarlyWork, RepoCollectionList result) {
      this.scholarlyWork = Objects.requireNonNull(scholarlyWork);
      this.result = Objects.requireNonNull(result);
    }

    public DoiBasedIdentity getDoi() {
      return scholarlyWork.getDoi();
    }

    public RepoVersion getVersion() {
      return result.getVersion();
    }
  }

  private PersistenceResult persist(ArticlePackage articlePackage) {
    ScholarlyWork articleWork = articlePackage.getArticleWork();
    RepoCollectionList articleInCrepo = persistToCrepo(articleWork);
    PersistedWork persistedArticle = new PersistedWork(articleWork, articleInCrepo);

    List<PersistedWork> persistedAssets = new ArrayList<>();
    for (ScholarlyWork assetWork : articlePackage.getAssetWorks()) {
      RepoCollectionList persistedAsset = persistToCrepo(assetWork);
      persistedAssets.add(new PersistedWork(assetWork, persistedAsset));
    }

    persistToSql(persistedArticle);
    for (PersistedWork persistedAsset : persistedAssets) {
      persistToSql(persistedAsset);
      persistRelation(persistedArticle.result, persistedAsset);
    }

    return new PersistenceResult(persistedArticle, persistedAssets);
  }

  private int persistToSql(PersistedWork persistedWork) {
    return parentService.hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "INSERT INTO scholarlyWork " +
          "(doi, crepoKey, crepoUuid, scholarlyWorkType) VALUES " +
          "(:doi, :crepoKey, :crepoUuid, :scholarlyWorkType)");
      query.setParameter("doi", persistedWork.scholarlyWork.getDoi().getIdentifier());
      query.setParameter("scholarlyWorkType", persistedWork.scholarlyWork.getType());

      RepoVersion repoVersion = persistedWork.result.getVersion();
      query.setParameter("crepoKey", repoVersion.getKey());
      query.setParameter("crepoUuid", repoVersion.getUuid().toString());

      return query.executeUpdate();
    });
  }

  private void persistRelation(RepoCollectionList article, PersistedWork asset) {
    parentService.hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "INSERT INTO scholarlyWorkRelation (originWorkId, targetWorkId, relationType) " +
          "VALUES (" +
          "  (SELECT scholarlyWorkId FROM scholarlyWork WHERE crepoKey=:originKey AND crepoUuid=:originUuid), " +
          "  (SELECT scholarlyWorkId FROM scholarlyWork WHERE crepoKey=:targetKey AND crepoUuid=:targetUuid), " +
          "  :relationType)");
      query.setParameter("relationType", "assetOf");

      RepoVersion articleVersion = article.getVersion();
      query.setParameter("originKey", articleVersion.getKey());
      query.setParameter("originUuid", articleVersion.getUuid().toString());

      RepoVersion assetVersion = asset.result.getVersion();
      query.setParameter("targetKey", assetVersion.getKey());
      query.setParameter("targetUuid", assetVersion.getUuid().toString());

      return query.executeUpdate();
    });
  }

  private RepoCollectionList persistToCrepo(ScholarlyWork work) {
    Map<String, RepoVersion> createdObjects = new LinkedHashMap<>();
    for (Map.Entry<String, RepoObject> entry : work.getObjects().entrySet()) {
      RepoObjectMetadata createdObject = parentService.contentRepoService.autoCreateRepoObject(entry.getValue());
      createdObjects.put(entry.getKey(), createdObject.getVersion());
    }

    String collectionMetadata = parentService.crepoGson.toJson(createdObjects);

    RepoCollection repoCollection = RepoCollection.builder()
        .setObjects(createdObjects.values())
        .setUserMetadata(collectionMetadata)
        .setKey(work.getCrepoKey())
        .build();

    return parentService.contentRepoService.autoCreateCollection(repoCollection);
  }

  private void persistRevision(PersistenceResult article, int revisionNumber) {
    DoiBasedIdentity articleDoi = article.getArticle().getDoi();

    // Delete assets
    parentService.hibernateTemplate.execute(session -> {
      Query query = session.createSQLQuery("" +
          "DELETE rev " +
          "FROM revision rev " +
          "INNER JOIN scholarlyWork asset ON rev.scholarlyWorkId = asset.scholarlyWorkId " +
          "INNER JOIN scholarlyWorkRelation rel ON asset.scholarlyWorkId = rel.targetWorkId " +
          "INNER JOIN scholarlyWork article ON article.scholarlyWorkId = rel.originWorkId " +
          "WHERE rev.revisionNumber = :revisionNumber AND article.doi = :doi");
      query.setParameter("revisionNumber", revisionNumber);
      query.setParameter("doi", articleDoi.getIdentifier());
      return query.executeUpdate();
    });

    // Delete root article
    parentService.hibernateTemplate.execute(session -> {
      Query query = session.createSQLQuery("" +
          "DELETE rev " +
          "FROM revision rev INNER JOIN scholarlyWork s " +
          "ON rev.scholarlyWorkId = s.scholarlyWorkId " +
          "WHERE rev.revisionNumber = :revisionNumber AND s.doi = :doi");
      query.setParameter("revisionNumber", revisionNumber);
      query.setParameter("doi", articleDoi.getIdentifier());
      return query.executeUpdate();
    });

    article.getWorks().forEachOrdered(work -> {
      parentService.hibernateTemplate.execute(session -> {
        Query query = session.createSQLQuery("" +
            "INSERT INTO revision (scholarlyWorkId, revisionNumber, publicationState) VALUES (" +
            "  (SELECT scholarlyWorkId FROM scholarlyWork WHERE crepoKey=:crepoKey AND crepoUuid=:crepoUuid), " +
            ":revisionNumber, :publicationState)");
        RepoVersion version = work.getVersion();
        query.setParameter("crepoKey", version.getKey());
        query.setParameter("crepoUuid", version.getUuid().toString());

        query.setParameter("revisionNumber", revisionNumber);
        query.setParameter("publicationState", 0);
        return query.executeUpdate();
      });
    });
  }

  private void stubAssociativeFields(Article article) {
    article.setID(-1L);
    article.setAssets(ImmutableList.of());
    article.setRelatedArticles(ImmutableList.of());
    article.setJournals(ImmutableSet.of());
    article.setCategories(ImmutableMap.of());
  }

  private ManifestXml.Asset findManuscriptAsset(List<ManifestXml.Asset> assets) {
    for (ManifestXml.Asset asset : assets) {
      if (asset.getMainEntry().isPresent()) {
        return asset;
      }
    }
    throw new RestClientException("main-entry not found", HttpStatus.BAD_REQUEST);
  }

  private ManifestXml.Representation findPrintableRepr(ManifestXml.Asset manuscriptAsset) {
    Optional<String> mainEntry = manuscriptAsset.getMainEntry();
    Preconditions.checkArgument(mainEntry.isPresent(), "manuscriptAsset must have main-entry");
    for (ManifestXml.Representation representation : manuscriptAsset.getRepresentations()) {
      if (representation.getName().equals("PDF")) {
        return representation;
      }
    }
    throw new RestClientException("main-entry not matched to asset", HttpStatus.BAD_REQUEST);
  }

  private ManifestXml.Representation findManuscriptRepr(ManifestXml.Asset manuscriptAsset) {
    Optional<String> mainEntry = manuscriptAsset.getMainEntry();
    Preconditions.checkArgument(mainEntry.isPresent(), "manuscriptAsset must have main-entry");
    for (ManifestXml.Representation representation : manuscriptAsset.getRepresentations()) {
      if (representation.getEntry().equals(mainEntry.get())) {
        return representation;
      }
    }
    throw new RestClientException("main-entry not matched to asset", HttpStatus.BAD_REQUEST);
  }


  private static final String MANUSCRIPTS_KEY = "manuscripts";
  private static final ImmutableBiMap<ArticleMetadataSource, String> SOURCE_KEYS = ImmutableBiMap.<ArticleMetadataSource, String>builder()
      .put(ArticleMetadataSource.FULL_MANUSCRIPT, "full")
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
   * <p>
   * The legacy Hibernate model object {@link Article} is used as a data-holder for convenience and compatibility. This
   * method constructs it anew, not by accessing Hibnerate, and populates only a subset of its normal fields.
   *
   * @param id     the ID of the article to serve
   * @param source whether to parse the extracted front matter or the full, original manuscript
   * @return an object containing metadata that could be extracted from the manuscript, with other fields unfilled
   * @deprecated method signature accommodates testing and will be changed
   */
  @Deprecated
  Article getArticleMetadata(ArticleIdentity id, OptionalInt revisionNumber, ArticleMetadataSource source) {
    /*
     * *** Implementation notes ***
     *
     * The method signature accommodates the methods `ArticleCrudController.previewMetadataFromVersionedModel` and
     * `ArticleCrudService.readVersionedMetadata`, which are temporary hacks to expose read-service functionality for
     * testing. This method's signature will probably change when those methods are removed, though we expect to keep
     * most of the business logic.
     *
     * The `source` argument ought to be replaced with something that doesn't expose so much detail. It gives the option
     * to parse the full manuscript (including the body) for validation purposes, which should not be possible in the
     * production implementation. We do currently plan to choose between the 'front' and 'frontAndBack' documents based
     * on whether we need to serve cited articles, but the signature should talk about whether to include citations, not
     * which file to read. In a future API version, we may wish to simplify further by splitting citations into a
     * separate service.
     *
     * TODO: Improve as described above and delete this comment block
     */

    RepoVersion collectionVersion = parentService.getRepoVersion(id, revisionNumber);

    RepoCollectionList collection = parentService.contentRepoService.getCollection(collectionVersion);

    Map<String, Object> userMetadata = (Map<String, Object>) collection.getJsonUserMetadata().get();
    RepoVersion manuscriptVersion = RepoVersionRepr.read((Map<?, ?>) userMetadata.get("manuscript"));

    Document document;
    try (InputStream manuscriptStream = parentService.contentRepoService.getRepoObject(manuscriptVersion)) {
      DocumentBuilder documentBuilder = AmbraService.newDocumentBuilder();
      log.debug("In getArticleMetadata source={} documentBuilder.parse() called", source);
      document = documentBuilder.parse(manuscriptStream);
      log.debug("finish");
    } catch (IOException | SAXException e) {
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
