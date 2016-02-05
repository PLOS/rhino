package org.ambraproject.rhino.service.impl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.ArticleRelationship;
import org.ambraproject.models.Category;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.AssetNodesByDoi;
import org.ambraproject.rhino.content.xml.AssetXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.DoiBasedCrudService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.service.article.NoSuchArticleIdException;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class LegacyIngestionService {

  private static final Logger log = LoggerFactory.getLogger(LegacyIngestionService.class);

  private final ArticleCrudServiceImpl parentService;

  LegacyIngestionService(ArticleCrudServiceImpl parentService) {
    this.parentService = Preconditions.checkNotNull(parentService);
  }

  /**
   * Query for an article by its identifier.
   *
   * @param id the article's identity
   * @return the article, or {@code null} if not found
   */
  public Article findArticleById(DoiBasedIdentity id) {
    return (Article) DataAccessUtils.uniqueResult((List<?>)
        parentService.hibernateTemplate.findByCriteria(DetachedCriteria
                .forClass(Article.class)
                .add(Restrictions.eq("doi", id.getKey()))
                .setFetchMode("articleType", FetchMode.JOIN)
                .setFetchMode("citedArticles", FetchMode.JOIN)
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
  }

  Article write(InputStream file, Optional<ArticleIdentity> suppliedId, DoiBasedCrudService.WriteMode mode) throws IOException {
    if (mode == null) {
      mode = DoiBasedCrudService.WriteMode.WRITE_ANY;
    }

    byte[] xmlData = parentService.readClientInput(file);
    Document doc = parentService.parseXml(xmlData);
    Article article = populateArticleFromXml(doc, suppliedId, mode, xmlData.length);
    persistArticle(article, xmlData);
    return article;
  }

  /**
   * Creates or updates an Article instance based on the given Document.  Does not persist the Article; that is the
   * responsibility of the caller.
   *
   * @param doc           Document describing the article XML
   * @param suppliedId    the indentifier supplied for the article by the external caller, if any
   * @param mode          whether to attempt a create or update
   * @param xmlDataLength the number of bytes in the uploaded XML file
   * @return the created Article
   * @throws IOException
   */
  private Article populateArticleFromXml(Document doc, Optional<ArticleIdentity> suppliedId,
                                         DoiBasedCrudService.WriteMode mode, int xmlDataLength) {
    return populateArticleFromXml(doc, Optional.<ManifestXml>absent(), suppliedId, mode, xmlDataLength);
  }

  /**
   * Creates or updates an Article instance based on the given Document.  Does not persist the Article; that is the
   * responsibility of the caller.
   *
   * @param doc           Document describing the article XML
   * @param manifestXml   The manifestXml document
   * @param suppliedId    the indentifier supplied for the article by the external caller, if any
   * @param mode          whether to attempt a create or update
   * @param xmlDataLength the number of bytes in the uploaded XML file
   * @return the created Article
   * @throws IOException
   */

  private Article populateArticleFromXml(Document doc, Optional<ManifestXml> manifestXml,
                                         Optional<ArticleIdentity> suppliedId,
                                         DoiBasedCrudService.WriteMode mode, int xmlDataLength) {
    ArticleXml xml = new ArticleXml(doc);
    ArticleIdentity doi;
    try {
      doi = xml.readDoi();
    } catch (XmlContentException e) {
      throw ArticleCrudServiceImpl.complainAboutXml(e);
    }

    if (suppliedId.isPresent() && !doi.equals(suppliedId.get())) {
      String message = String.format("Article XML with DOI=\"%s\" uploaded to mismatched address: \"%s\"",
          doi.getIdentifier(), suppliedId.get().getIdentifier());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }

    Article article = findArticleById(doi);
    final boolean creating = (article == null);
    if ((creating && mode == DoiBasedCrudService.WriteMode.UPDATE_ONLY) || (!creating && mode == DoiBasedCrudService.WriteMode.CREATE_ONLY)) {
      String messageStub = (creating ?
          "Can't update; article does not exist at " : "Can't create; article already exists at ");
      throw new RestClientException(messageStub + doi.getIdentifier(), HttpStatus.METHOD_NOT_ALLOWED);
    }
    if (creating) {
      article = new Article();
      article.setDoi(doi.getKey());
    }

    try {
      article = xml.build(article);
    } catch (XmlContentException e) {
      throw ArticleCrudServiceImpl.complainAboutXml(e);
    }

    // TODO: If an article should have multiple journals, how does it get them?
    article.setJournals(Sets.newHashSet(parentService.getPublicationJournal(article)));
    populateCategories(article, doc);
    initializeAssets(article, manifestXml, xml, xmlDataLength);
    populateRelatedArticles(article, xml);

    return article;
  }

  /**
   * Saves the hibernate entity representing an article.
   *
   * @param article the new or updated Article instance to save
   */
  private void saveArticleToHibernate(Article article) {
    if (article.getID() == null) {
      parentService.hibernateTemplate.save(article);
    } else {
      parentService.hibernateTemplate.update(article);
    }
  }

  /**
   * Saves both the hibernate entity and the bytes representing an article.
   *
   * @param article the new or updated Article instance to save
   * @param xmlData bytes of the article XML file to save
   * @throws IOException
   */
  private void persistArticle(Article article, byte[] xmlData) throws IOException {
    saveArticleToHibernate(article);
    String doi = article.getDoi();

    createReciprocalRelationships(article);
    try {

      // This method needs the article to have already been persisted to the DB.
      parentService.syndicationService.createSyndications(doi);
    } catch (NoSuchArticleIdException nsaide) {

      // Should never happen, since we have already loaded this article.
      throw new RuntimeException(nsaide);
    }

    // ArticleIdentity doesn't like this part of the DOI.
    doi = doi.substring("info:doi/".length());
    AssetFileIdentity xmlIdentity = ArticleIdentity.create(doi).forXmlAsset();
    parentService.write(xmlData, xmlIdentity);
  }

  /**
   * Set reciprocal {@link org.ambraproject.models.ArticleRelationship} values on an article that has just been
   * ingested.
   * <p>
   * The argument is an article in the middle of being ingested, and should already be persisted. This method is a
   * secondary step in ingestion.
   *
   * @param ingested a newly ingested article
   */
  private void createReciprocalRelationships(Article ingested) {
    Preconditions.checkState(ingested.getID() != null, "Article must have already been persisted");

    List<ArticleRelationship> relationships = ingested.getRelatedArticles();
    Set<String> relatedArticleIds = Sets.newHashSetWithExpectedSize(relationships.size());

    // Reciprocate relationships from ingested to other articles
    for (ArticleRelationship relationship : relationships) {
      relatedArticleIds.add(relationship.getOtherArticleDoi());
      reciprocateOutboundRelationship(ingested, relationship);
    }

    // Reciprocate relationships from other articles to ingested
    DetachedCriteria inboundCriteria = DetachedCriteria.forClass(Article.class);
    if (!relatedArticleIds.isEmpty()) {
      inboundCriteria = inboundCriteria.add(Restrictions.not(Restrictions.in("doi", relatedArticleIds)));
    }
    inboundCriteria = inboundCriteria.createCriteria("relatedArticles")
        .add(Restrictions.eq("otherArticleDoi", ingested.getDoi()));
    List<Article> articlesWithInboundRelationships = (List<Article>) parentService.hibernateTemplate.findByCriteria(inboundCriteria);
    if (!articlesWithInboundRelationships.isEmpty()) {
      for (Article inboundArticle : articlesWithInboundRelationships) {
        reciprocateInboundRelationship(ingested, inboundArticle);
      }
      parentService.hibernateTemplate.update(ingested);
    }
  }

  /**
   * Find a relationship from one article to another, if it is stored in the source's related articles. Return null if
   * none exists.
   */
  private static ArticleRelationship findRelationshipTo(Article source, Article target) {
    for (ArticleRelationship relationship : source.getRelatedArticles()) {
      if (relationship.getOtherArticleDoi().equals(target.getDoi())) {
        return relationship;
      }
    }
    return null;
  }

  /**
   * Special cases of article relationship types where the reciprocal relationships are asymmetric.
   * <p>
   * For example, if A is a "retracted-article" of B, then B must be a "retraction" of A.
   * <p>
   * TODO: Reduce code duplication with org.ambraproject.article.service.IngesterImpl.RECIPROCAL_TYPES
   */
  private static final ImmutableBiMap<String, String> RECIPROCAL_TYPES = ImmutableBiMap.<String, String>builder()
      .put("corrected-article", "correction-forward")
      .put("retracted-article", "retraction")
      .put("object-of-concern", "expressed-concern")
      .build();

  /**
   * Set up a reciprocal relationship from {@code parentArticle} to {@code otherArticle}. That is, assuming {@code
   * relationship} is a pre-existing relationship from {@code otherArticle} to {@code parentArticle}, this method
   * creates a new relationship that reciprocates it.
   * <p>
   * The type of the new, reciprocal relationship is the same as that of the first relationship unless it is one of the
   * asymmetric relations described in {@link #RECIPROCAL_TYPES}.
   */
  private static void reciprocate(ArticleRelationship relationship, Article parentArticle, Article otherArticle) {
    ArticleRelationship reciprocal = new ArticleRelationship();
    reciprocal.setParentArticle(parentArticle);
    reciprocal.setOtherArticleID(otherArticle.getID());
    reciprocal.setOtherArticleDoi(otherArticle.getDoi());

    String relationshipType = relationship.getType();
    reciprocal.setType(getReciprocalType(relationshipType));

    parentArticle.getRelatedArticles().add(reciprocal);
  }

  private static String getReciprocalType(String relationshipType) {
    String reciprocalType = RECIPROCAL_TYPES.get(relationshipType);
    return (reciprocalType == null) ? relationshipType : reciprocalType;
  }

  /**
   * For a relationship from a new article to an old one, set up a reciprocal relationship from the old one back to the
   * new one.
   *
   * @param ingested     a newly ingested article
   * @param relationship a relationship from {@code ingested} to a pre-existing article
   */
  private void reciprocateOutboundRelationship(Article ingested, ArticleRelationship relationship) {
    Article relatedArticle = (Article) DataAccessUtils.uniqueResult(
        parentService.hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
            .add(Restrictions.eq("doi", relationship.getOtherArticleDoi())))
    );
    if (relatedArticle == null) {
      return; // The referenced article does not exist in the system, so do nothing.
    }
    relationship.setOtherArticleID(relatedArticle.getID());
    parentService.hibernateTemplate.update(relationship);

    ArticleRelationship reciprocal = findRelationshipTo(relatedArticle, ingested);
    if (reciprocal != null) {
      reciprocal.setOtherArticleID(ingested.getID());
      reciprocal.setType(getReciprocalType(relationship.getType()));
      parentService.hibernateTemplate.update(reciprocal);
    } else {
      reciprocate(relationship, relatedArticle, ingested);
      parentService.hibernateTemplate.update(relatedArticle);
    }
  }

  /**
   * For a pre-existing relationship from an article to a newly ingested article, set up a reciprocal relationship from
   * the new article back to the old one.
   *
   * @param ingested       a newly ingested article
   * @param inboundArticle a pre-existing article that defines a relationship to {@code ingested} by its DOI
   */
  private void reciprocateInboundRelationship(Article ingested, Article inboundArticle) {
    ArticleRelationship inboundRelationship = findRelationshipTo(inboundArticle, ingested);
    if (inboundRelationship == null) {
      throw new IllegalArgumentException("Inbound article has no relationship to ingested article");
    }
    inboundRelationship.setOtherArticleID(ingested.getID());
    parentService.hibernateTemplate.update(inboundRelationship);

    reciprocate(inboundRelationship, ingested, inboundArticle);
  }


  Article writeArchive(Archive archive, Optional<ArticleIdentity> suppliedId, DoiBasedCrudService.WriteMode mode)
      throws IOException {
    Document manifestDoc = getManifest(archive);
    ManifestXml manifest = new ManifestXml(manifestDoc);

    byte[] xmlData;
    try (InputStream xmlStream = archive.openFile(manifest.getArticleXml())) {
      xmlData = ByteStreams.toByteArray(xmlStream);
    }
    Document doc = parentService.parseXml(xmlData);
    Article article = populateArticleFromXml(doc, Optional.fromNullable(manifest), suppliedId, mode, xmlData.length);
    article.setArchiveName(new File(archive.getArchiveName()).getName());
    article.setStrkImgURI(manifest.getStrkImgURI());

    // Save now, before we add asset files, since AssetCrudServiceImpl will expect the
    // Article to be persisted at this point.
    persistArticle(article, xmlData);
    try {
      addAssetFiles(article, archive, manifest);

      /*
       * Refresh the article in order to force it contain any new asset file objects that might have been inserted into
       * the database without being reflected in Hibernate. See the raw-SQL kludge in
       * org.ambraproject.rhino.service.impl.AssetCrudServiceImpl.saveAssetForcingParentArticle
       * for one possible cause.
       *
       * MUST flush before refreshing. Otherwise, updates in the Hibernate buffer may be dropped unsaved. Observed in
       * at least one case, with the "parentService.hibernateTemplate.update" statement in
       * org.ambraproject.rhino.service.impl.AssetCrudServiceImpl.upload
       * not being persisted. That's bad.
       */
      parentService.hibernateTemplate.flush();
      parentService.hibernateTemplate.refresh(article);
    } catch (RestClientException rce) {
      try {
        // If there is an error processing the assets, delete the article we created
        // above, since it won't be valid.
        parentService.delete(ArticleIdentity.create(article));
      } catch (RuntimeException exceptionOnDeletion) {
        exceptionOnDeletion.addSuppressed(rce);
        throw exceptionOnDeletion;
      }
      throw rce;
    }
    return article;
  }

  /**
   * Determines if we should save a file contained within an article archive as an asset.
   *
   * @param filename           the name of a file within a .zip archive
   * @param articleXmlFilename the file name for the article XML, as given by the manifest (must be lowercase)
   * @return true if this file should be persisted as an asset
   */
  static boolean shouldSaveAssetFile(String filename, String articleXmlFilename) {
    Preconditions.checkNotNull(filename);
    filename = filename.toLowerCase().trim();
    return !(filename.startsWith("manifest.") || filename.startsWith(articleXmlFilename));
  }

  private void addAssetFiles(Article article, Archive zipFile, ManifestXml manifest)
      throws IOException {
    String articleXmlFilename = manifest.getArticleXml().toLowerCase();

    // TODO: remove existing files if this is a reingest (see IngesterImpl.java line 324)

    for (String filename : zipFile.getEntryNames()) {
      if (shouldSaveAssetFile(filename, articleXmlFilename)) {
        String[] fields = filename.split("\\.");

        // Not sure why, but the existing admin code always converts the extension to UPPER.
        String extension = fields[fields.length - 1].toUpperCase();
        String doi = manifest.getUriForFile(filename);
        if (doi == null) {
          throw new RestClientException("File does not appear in manifest: " + filename,
              HttpStatus.METHOD_NOT_ALLOWED);
        }

        try (InputStream is = zipFile.openFile(filename)) {
          parentService.assetService.upload(is, AssetFileIdentity.create(doi, extension));
        }
      }
    }
  }

  /**
   * Returns the manifest from the archive as an XML Document.
   *
   * @param archive zip archive
   * @return Document view of the manifest
   * @throws IOException
   */
  private Document getManifest(Archive archive) throws IOException {
    for (String entryName : archive.getEntryNames()) {
      if ("MANIFEST.xml".equalsIgnoreCase(entryName)) {
        return AmbraService.parseXml(archive.openFile(entryName));
      }
    }
    throw new RestClientException("No manifest found in archive " + archive.getArchiveName(),
        HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * Reads and returns the contents of a file in a .zip archive.
   *
   * @param zipFile  zip file
   * @param filename name of the file within the archive to read
   * @return contents of the file, or null if the file does not exist in zipFile
   * @throws IOException
   */
  private byte[] readZipFile(ZipFile zipFile, String filename) throws IOException {
    ZipEntry entry = zipFile.getEntry(filename);
    if (entry == null) {
      return null;
    }
    try (InputStream is = zipFile.getInputStream(entry)) {
      return parentService.readClientInput(is);
    }
  }

  void repopulateCategories(ArticleIdentity id) throws IOException {
    Document doc = parentService.parseXml(parentService.readXml(id));
    Article article = findArticleById(id);
    populateCategories(article, doc);
    if (article.getCategories().size() > 0) {
      saveArticleToHibernate(article);
    } else {
      throw new IOException("Taxonomy server returned 0 terms. Cannot repopulate Categories. "
          + article.getDoi());
    }
  }

  /**
   * Populates article category information by making a call to the taxonomy server.
   *
   * @param article the Article model instance
   * @param xml     Document representing the article XML
   */
  private void populateCategories(Article article, Document xml) {

    // Attempt to assign categories to the non-amendment article based on the taxonomy server.  However,
    // we still want to ingest the article even if this process fails.
    Map<String, Integer> terms;

    try {
      if (!parentService.articleService.isAmendment(article)) {
        terms = parentService.taxonomyService.classifyArticle(xml, article);
        if (terms != null && terms.size() > 0) {
          parentService.articleService.setArticleCategories(article, terms);
        } else {
          article.setCategories(new HashMap<Category, Integer>());
        }
      } else {
        article.setCategories(new HashMap<Category, Integer>());
      }
    } catch (TaxonomyClassificationService.TaxonomyClassificationServiceNotConfiguredException e) {
      log.error("Taxonomy server not configured. Ingesting article without categories. " + article.getDoi(), e);
    } catch (Exception e) {
      log.error("Taxonomy server not responding, but ingesting article anyway." + article.getDoi(), e);
    }
  }

  private void initializeAssets(final Article article, Optional<ManifestXml> manifestXml, ArticleXml xml, int xmlDataLength) {
    AssetNodesByDoi assetNodes = xml.findAllAssetNodes();
    List<ArticleAsset> assets = article.getAssets();
    Collection<String> assetDois = assetNodes.getDois();
    String strikingImageDOI = null;

    //Get the striking image DOI for the assets, a fix for:
    //BAU-4.
    //
    //It's possible that this method was called for ingesting a new version of the article XML
    //without the manifest.  In this case we want to be sure to not delete the striking
    //image from the database
    //
    //This should cover 6 use cases:
    //There is no striking image (for update and create)
    //Striking image defined in article XML, but not a special asset (for update and create)
    //Striking image defined in manifest, and as a special asset (for update and create)

    if (manifestXml.isPresent()) {
      strikingImageDOI = manifestXml.get().getStrkImgURI();
    } else {
      strikingImageDOI = article.getStrkImgURI();

      if (strikingImageDOI == null) {
        if (assets != null) {
          //One last check of the existing database rows.
          for (ArticleAsset asset : assets) {
            //TODO: There should be some way to specify an asset in the asset table as the striking image
            if (asset.getDoi().contains(".strk.")) {
              strikingImageDOI = asset.getDoi();
              break;
            }
          }
        }
      }
    }

    if (assets == null) {  // create
      assets = Lists.newArrayListWithCapacity(assetDois.size());
      article.setAssets(assets);
    } else {  // update

      // Ugly hack copied from the old admin code.  The problem is that hibernate, when it
      // eventually commits this transaction, will insert new articleAsset rows before it
      // deletes the old ones, leading to a MySQL unique constraint violation.  I've tried
      // many, many variations of doing this in hibernate only, without falling back to
      // JDBC, involving flushing and clearing the session in various orders.  However
      // they all lead to either unique constraint violations or optimistic locking
      // exceptions.
      parentService.hibernateTemplate.execute(new HibernateCallback<Integer>() {
        @Override
        public Integer doInHibernate(Session session) throws HibernateException, SQLException {
          return session.createSQLQuery(
              "update articleAsset " +
                  "set doi = concat('old-', doi), " +
                  "extension = concat('old-', extension) " +
                  "where articleID = :articleID"
          ).setParameter("articleID", article.getID())
              .executeUpdate();
        }
      });
      assets.clear();
    }

    for (String assetDoi : assetDois) {
      ArticleAsset asset;
      try {
        asset = parseAsset(assetNodes, assetDoi);
      } catch (XmlContentException e) {
        throw ArticleCrudServiceImpl.complainAboutXml(e);
      }
      assets.add(asset);
    }

    //Add the striking image to the assets, a fix for:
    //BAU-4
    if (strikingImageDOI != null) {
      //Check to make sure the asset doesn't exist already
      //Sometimes the striking image is a regular image
      boolean found = false;
      for (ArticleAsset asset : assets) {
        if (asset.getDoi().equals(strikingImageDOI)) {
          found = true;
          break;
        }
      }

      if (!found) {
        ArticleAsset strkImageAsset = new ArticleAsset();
        strkImageAsset.setDoi(strikingImageDOI);
        strkImageAsset.setExtension("");
        strkImageAsset.setTitle("");
        strkImageAsset.setDescription("");
        strkImageAsset.setContextElement("");
        assets.add(strkImageAsset);
        log.debug("Added striking image, DOI: {}", strikingImageDOI);
      } else {
        log.debug("Used existing striking image, DOI: {}", strikingImageDOI);
      }
    }

    ArticleAsset xmlAsset = new ArticleAsset();
    xmlAsset.setDoi(article.getDoi());
    xmlAsset.setExtension("XML");
    xmlAsset.setTitle(article.getTitle());
    xmlAsset.setDescription(article.getDescription());
    xmlAsset.setContentType("text/xml");
    xmlAsset.setSize(xmlDataLength);
    assets.add(xmlAsset);
  }

  private void populateRelatedArticles(Article article, ArticleXml xml) {
    List<ArticleRelationship> xmlRelationships = xml.parseRelatedArticles();
    if (article.getRelatedArticles() == null) {
      // Ingesting for the first time
      article.setRelatedArticles(xmlRelationships);
    } else {
      // Re-ingesting. Modify persistent values in place.
      modifyRelatedArticles(article.getRelatedArticles(), xmlRelationships);
    }

    for (ArticleRelationship relationship : article.getRelatedArticles()) {
      relationship.setParentArticle(article);

      String otherArticleDoi = relationship.getOtherArticleDoi();
      Article otherArticle = (Article) DataAccessUtils.uniqueResult((List<?>) parentService.hibernateTemplate.findByCriteria(
          DetachedCriteria.forClass(Article.class)
              .add(Restrictions.eq("doi", otherArticleDoi))
      ));
      relationship.setOtherArticleID(otherArticle == null ? null : otherArticle.getID());
    }
  }

  /**
   * Modify persistent values in place to match XML. This is a workaround to avoid relying on Hibernate's cascading
   * delete behavior, with would normally let us just remove the old list and replace it with a new one. As a kludge, if
   * a new value has the same "other article DOI" as an old one, modify the old one to match the new one instead of
   * replacing it.
   * <p>
   * TODO: Un-kludge
   *
   * @param persistentList the list to modify
   * @param parsedList     the list of desired values
   */
  private void modifyRelatedArticles(List<ArticleRelationship> persistentList, List<ArticleRelationship> parsedList) {
    // Map preexisting relationships by their target DOI
    Map<String, ArticleRelationship> preexistingMap = Maps.newHashMapWithExpectedSize(persistentList.size());
    for (ArticleRelationship preexisting : persistentList) {
      ArticleRelationship previous = preexistingMap.put(preexisting.getOtherArticleDoi(), preexisting);
      if (previous != null) {
        log.warn("Multiple relationships to {}", preexisting.getOtherArticleDoi());
        // Leave the other one to be modified.
        // In case there should actually be more than one, the others will be created new after the first is modified.
        parentService.hibernateTemplate.delete(previous);
      }
    }

    // For each relationship in XML, modify the preexisting one in place, or add a new one if there is none
    for (ArticleRelationship parsed : parsedList) {
      String target = parsed.getOtherArticleDoi();
      ArticleRelationship preexisting = preexistingMap.remove(target);
      if (preexisting != null) {
        preexisting.setType(parsed.getType());
      } else {
        persistentList.add(parsed);
      }
    }

    /*
     * Delete preexisting relationships not found in the present XML.
     *
     * This deletes relationships that were created from the XML in a previous ingestion but deleted in this draft.
     * It also deletes reciprocal relationships created from the XML of other articles that refer to this one,
     * but those will be created again in createReciprocalRelationships.
     */
    persistentList.removeAll(preexistingMap.values());
  }

  /**
   * Write metadata from an XML node describing an asset into a new asset entity.
   *
   * @param assetNodes the set of all asset nodes found in the article document
   * @param assetDoi   the DOI of the asset to find and write
   * @return the new asset
   */
  private static ArticleAsset parseAsset(AssetNodesByDoi assetNodes, String assetDoi) throws XmlContentException {
    AssetIdentity assetIdentity = AssetIdentity.create(assetDoi);
    List<Node> matchingNodes = assetNodes.getNodes(assetDoi);

    /*
     * In the typical case, there is exactly one matching node.
     *
     * Rarely, multiple nodes will have the same DOI (such as two representations of the same "supplemental info"
     * asset). Legacy behavior in this case is to store metadata for the first one.
     */
    if (matchingNodes.size() > 1) {
      log.warn("Matched multiple nodes with DOI=\"{}\"; defaulting to first instance", assetDoi);
    }
    return parseAssetNode(matchingNodes.get(0), assetIdentity);
  }

  private static ArticleAsset parseAssetNode(Node assetNode, AssetIdentity assetIdentity) throws XmlContentException {
    return new AssetXml(assetNode, assetIdentity).build(new ArticleAsset());
  }

  public Archive repack(ArticleIdentity articleIdentity) {
    Article article = findArticleById(articleIdentity);
    if (article == null) {
      throw new RestClientException("Article to repack not found: " + articleIdentity, HttpStatus.NOT_FOUND);
    }
    ImmutableMap.Builder<String, Archive.InputStreamSource> map = ImmutableMap.builder();

    map.put("manifest.dtd", MANIFEST_SOURCE);
    map.put("MANIFEST.xml", rebuildManifest(article));

    for (ArticleAsset articleAsset : article.getAssets()) {
      final AssetFileIdentity assetFileIdentity = AssetFileIdentity.from(articleAsset);
      String entryName = inferFileName(assetFileIdentity) + "." + assetFileIdentity.getFileExtension();
      map.put(entryName, new Archive.InputStreamSource() {
        @Override
        public InputStream open() throws IOException {
          return parentService.assetService.read(assetFileIdentity);
        }
      });
    }

    String archiveName = inferFileName(articleIdentity) + ".zip";
    return Archive.pack(archiveName, map.build());
  }

  private static final Archive.InputStreamSource MANIFEST_SOURCE = new Archive.InputStreamSource() {
    @Override
    public InputStream open() throws IOException {
      return getClass().getClassLoader().getResourceAsStream("manifest.dtd");
    }
  };

  private static Archive.InputStreamSource rebuildManifest(Article article) {
    final String articleDoi = article.getDoi();

    Document manifest;
    try {
      manifest = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }

    Element manifestElement = (Element) manifest.appendChild(manifest.createElement("manifest"));
    Element articleBundle = (Element) manifestElement.appendChild(manifest.createElement("articleBundle"));

    String articleNameStub = inferFileName(ArticleIdentity.create(article));
    String xmlEntryName = articleNameStub + ".XML";
    Element articleElement = (Element) articleBundle.appendChild(manifest.createElement("article"));
    articleElement.setAttribute("uri", articleDoi);
    articleElement.setAttribute("main-entry", xmlEntryName);
    Element xmlRepr = (Element) articleElement.appendChild(manifest.createElement("representation"));
    xmlRepr.setAttribute("name", "XML");
    xmlRepr.setAttribute("entry", xmlEntryName);

    if (articleHasPdfRepresentation(article)) {
      Element pdfRepr = (Element) articleElement.appendChild(manifest.createElement("representation"));
      pdfRepr.setAttribute("name", "PDF");
      pdfRepr.setAttribute("entry", articleNameStub + ".PDF");
    }

    ListMultimap<String, ArticleAsset> assetsByDoi = Multimaps.index(article.getAssets(), new Function<ArticleAsset, String>() {
      @Override
      public String apply(ArticleAsset input) {
        return input.getDoi();
      }
    });
    for (Map.Entry<String, List<ArticleAsset>> assetGroup : Multimaps.asMap(assetsByDoi).entrySet()) {
      String key = assetGroup.getKey();
      if (key.equals(articleDoi)) {
        // These assets were represented above as the <article> element.
        // There should not be a second <object> element for them.
        continue;
      }

      Element objectElement = (Element) articleBundle.appendChild(manifest.createElement("object"));
      objectElement.setAttribute("uri", key);

      if (key.equals(article.getStrkImgURI())) {
        objectElement.setAttribute("strkImage", "True");
      }

      for (ArticleAsset asset : assetGroup.getValue()) {
        Element reprElement = (Element) objectElement.appendChild(manifest.createElement("representation"));
        reprElement.setAttribute("name", asset.getExtension());

        // TODO: Deduplicate code from 'repack' method
        AssetFileIdentity assetFileIdentity = AssetFileIdentity.from(asset);
        String entryName = inferFileName(assetFileIdentity) + "." + assetFileIdentity.getFileExtension();

        reprElement.setAttribute("entry", entryName);
      }
    }

    String comment = "Repacked at " + new Date();
    manifestElement.insertBefore(manifest.createComment(comment), articleBundle);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "manifest.dtd");
      transformer.transform(new DOMSource(manifest), new StreamResult(outputStream));
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    final byte[] result = outputStream.toByteArray();

    return new Archive.InputStreamSource() {
      @Override
      public InputStream open() throws IOException {
        return new ByteArrayInputStream(result);
      }
    };
  }

  private static final Pattern PLOS_DOI_NAMING_CONVENTION = Pattern.compile(
      "info:doi/10\\.\\d+/(?:journal\\.)?(\\w+(?:\\.\\w+)*)");

  /**
   * Generate a file name, to be used as a zip archive entry name, for an article or asset.
   * <p>
   * The legacy data model gives us no way to recover the actual file name from the original archive. PLOS gets
   * preferential treatment here -- if a DOI matches PLOS's naming convention, apply PLOS's corresponding convention for
   * file names. Otherwise, return the last slash-delimited token.
   *
   * @param identity an identifier for an article or asset
   * @return a filename with no extension for representing the argument
   */
  private static String inferFileName(DoiBasedIdentity identity) {
    Matcher matcher = PLOS_DOI_NAMING_CONVENTION.matcher(identity.getKey());
    return matcher.matches() ? matcher.group(1) : identity.getLastToken();
  }

  /**
   * Iterate through an article's assets to determine if the article has a PDF representation The PDF asset DOI is equal
   * to the article DOI, and the extension is "PDF".
   *
   * @param article the article to analyze
   * @return boolean indicating existence of article PDF representation
   */
  private static boolean articleHasPdfRepresentation(Article article) {
    for (ArticleAsset articleAsset : article.getAssets()) {
      if (articleAsset.getDoi().equals(article.getDoi())
          && articleAsset.getExtension().equals("PDF")) {
        return true;
      }
    }
    return false;
  }
}
