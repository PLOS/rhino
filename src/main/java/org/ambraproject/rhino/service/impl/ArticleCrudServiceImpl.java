/*
 * Copyright (c) 2006-2014 by Public Library of Science
 *
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambraproject.rhino.service.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.ArticleRelationship;
import org.ambraproject.models.Category;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.AssetNodesByDoi;
import org.ambraproject.rhino.content.xml.AssetXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.PingbackReadService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleAuthorView;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.ambraproject.rhino.view.article.ArticleOutputView;
import org.ambraproject.rhino.view.article.ArticleOutputViewFactory;
import org.ambraproject.rhino.view.article.RelatedArticleView;
import org.ambraproject.service.article.NoSuchArticleIdException;
import org.ambraproject.views.AuthorView;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Service implementing _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
public class ArticleCrudServiceImpl extends AmbraService implements ArticleCrudService {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudServiceImpl.class);

  private AssetCrudService assetService;

  @Autowired
  protected PingbackReadService pingbackReadService;
  @Autowired
  private ArticleOutputViewFactory articleOutputViewFactory;
  @Autowired
  private XpathReader xpathReader;
  @Autowired
  private TaxonomyService taxonomyService;

  private boolean articleExistsAt(DoiBasedIdentity id) {
    DetachedCriteria criteria = DetachedCriteria.forClass(Article.class)
        .add(Restrictions.eq("doi", id.getKey()));
    return exists(criteria);
  }

  /**
   * Query for an article by its identifier.
   *
   * @param id the article's identity
   * @return the article, or {@code null} if not found
   */
  public Article findArticleById(DoiBasedIdentity id) {
    return (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
                .forClass(Article.class)
                .add(Restrictions.eq("doi", id.getKey()))
                .setFetchMode("articleType", FetchMode.JOIN)
                .setFetchMode("citedArticles", FetchMode.JOIN)
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Article write(InputStream file, Optional<ArticleIdentity> suppliedId, WriteMode mode) throws IOException {
    if (mode == null) {
      mode = WriteMode.WRITE_ANY;
    }

    byte[] xmlData = readClientInput(file);
    Document doc = parseXml(xmlData);
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
                                         WriteMode mode, int xmlDataLength) {
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
                                         WriteMode mode, int xmlDataLength) {
    ArticleXml xml = new ArticleXml(doc);
    ArticleIdentity doi;
    try {
      doi = xml.readDoi();
    } catch (XmlContentException e) {
      throw complainAboutXml(e);
    }

    if (suppliedId.isPresent() && !doi.equals(suppliedId.get())) {
      String message = String.format("Article XML with DOI=\"%s\" uploaded to mismatched address: \"%s\"",
          doi.getIdentifier(), suppliedId.get().getIdentifier());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }

    Article article = findArticleById(doi);
    final boolean creating = (article == null);
    if ((creating && mode == WriteMode.UPDATE_ONLY) || (!creating && mode == WriteMode.CREATE_ONLY)) {
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
      throw complainAboutXml(e);
    }
    relateToJournals(article);
    populateCategories(article, doc);
    initializeAssets(article, manifestXml, xml, xmlDataLength);
    populateRelatedArticles(article, xml);

    return article;
  }

  /**
   * Saves both the hibernate entity and the bytes representing an article.
   *
   * @param article the new or updated Article instance to save
   * @param xmlData bytes of the article XML file to save
   * @throws IOException
   */
  private void persistArticle(Article article, byte[] xmlData)
      throws IOException {
    if (article.getID() == null) {
      hibernateTemplate.save(article);
    } else {
      hibernateTemplate.update(article);
    }
    String doi = article.getDoi();

    createReciprocalRelationships(article);
    try {

      // This method needs the article to have already been persisted to the DB.
      syndicationService.createSyndications(doi);
    } catch (NoSuchArticleIdException nsaide) {

      // Should never happen, since we have already loaded this article.
      throw new RuntimeException(nsaide);
    }

    // ArticleIdentity doesn't like this part of the DOI.
    doi = doi.substring("info:doi/".length());
    AssetFileIdentity xmlIdentity = ArticleIdentity.create(doi).forXmlAsset();
    write(xmlData, xmlIdentity);
  }

  /**
   * Set reciprocal {@link ArticleRelationship} values on an article that has just been ingested.
   * <p/>
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
    List<Article> articlesWithInboundRelationships = hibernateTemplate.findByCriteria(inboundCriteria);
    if (!articlesWithInboundRelationships.isEmpty()) {
      for (Article inboundArticle : articlesWithInboundRelationships) {
        reciprocateInboundRelationship(ingested, inboundArticle);
      }
      hibernateTemplate.update(ingested);
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
   * <p/>
   * For example, if A is a "retracted-article" of B, then B must be a "retraction" of A.
   * <p/>
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
   * <p/>
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
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
            .add(Restrictions.eq("doi", relationship.getOtherArticleDoi())))
    );
    if (relatedArticle == null) {
      return; // The referenced article does not exist in the system, so do nothing.
    }
    relationship.setOtherArticleID(relatedArticle.getID());
    hibernateTemplate.update(relationship);

    ArticleRelationship reciprocal = findRelationshipTo(relatedArticle, ingested);
    if (reciprocal != null) {
      reciprocal.setOtherArticleID(ingested.getID());
      reciprocal.setType(getReciprocalType(relationship.getType()));
      hibernateTemplate.update(reciprocal);
    } else {
      reciprocate(relationship, relatedArticle, ingested);
      hibernateTemplate.update(relatedArticle);
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
    hibernateTemplate.update(inboundRelationship);

    reciprocate(inboundRelationship, ingested, inboundArticle);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public Article writeArchive(String filename, Optional<ArticleIdentity> suppliedId, WriteMode mode)
      throws IOException {
    ZipFile zip = new ZipFile(filename);
    Document manifestDoc = getManifest(zip);
    ManifestXml manifest = new ManifestXml(manifestDoc);
    byte[] xmlData = readZipFile(zip, manifest.getArticleXml());
    Document doc = parseXml(xmlData);
    Article article = populateArticleFromXml(doc, Optional.fromNullable(manifest), suppliedId, mode, xmlData.length);
    article.setArchiveName(new File(filename).getName());
    article.setStrkImgURI(manifest.getStrkImgURI());

    // Save now, before we add asset files, since AssetCrudServiceImpl will expect the
    // Article to be persisted at this point.
    persistArticle(article, xmlData);
    try {
      addAssetFiles(article, zip, manifest);

      /*
       * Refresh the article in order to force it contain any new asset file objects that might have been inserted into
       * the database without being reflected in Hibernate. See the raw-SQL kludge in
       * org.ambraproject.rhino.service.impl.AssetCrudServiceImpl.saveAssetForcingParentArticle
       * for one possible cause.
       *
       * MUST flush before refreshing. Otherwise, updates in the Hibernate buffer may be dropped unsaved. Observed in
       * at least one case, with the "hibernateTemplate.update" statement in
       * org.ambraproject.rhino.service.impl.AssetCrudServiceImpl.upload
       * not being persisted. That's bad.
       */
      hibernateTemplate.flush();
      hibernateTemplate.refresh(article);
    } catch (RestClientException rce) {
      try {
        // If there is an error processing the assets, delete the article we created
        // above, since it won't be valid.
        delete(ArticleIdentity.create(article));
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
  @VisibleForTesting
  public static boolean shouldSaveAssetFile(String filename, String articleXmlFilename) {
    Preconditions.checkNotNull(filename);
    filename = filename.toLowerCase().trim();
    return !(filename.startsWith("manifest.") || filename.startsWith(articleXmlFilename));
  }

  private void addAssetFiles(Article article, ZipFile zipFile, ManifestXml manifest)
      throws IOException {
    String articleXmlFilename = manifest.getArticleXml().toLowerCase();

    // TODO: remove existing files if this is a reingest (see IngesterImpl.java line 324)

    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      String filename = entry.getName();
      if (shouldSaveAssetFile(filename, articleXmlFilename)) {
        String[] fields = filename.split("\\.");

        // Not sure why, but the existing admin code always converts the extension to UPPER.
        String extension = fields[fields.length - 1].toUpperCase();
        String doi = manifest.getUriForFile(filename);
        if (doi == null) {
          throw new RestClientException("File does not appear in manifest: " + filename,
              HttpStatus.METHOD_NOT_ALLOWED);
        }

        try (InputStream is = zipFile.getInputStream(entry)) {
          assetService.upload(is, AssetFileIdentity.create(doi, extension));
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
  private Document getManifest(ZipFile archive) throws IOException {
    byte[] bytes = readZipFile(archive, "MANIFEST.xml");
    if (bytes == null) {

      // I've seen a few archives with the name in lowercase.  Not common.
      bytes = readZipFile(archive, "manifest.xml");
      if (bytes == null) {
        throw new RestClientException("No manifest found in archive " + archive.getName(),
            HttpStatus.METHOD_NOT_ALLOWED);
      }
    }
    return parseXml(bytes);
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
      return readClientInput(is);
    }
  }

  /**
   * Populate an article's {@code journals} field with {@link Journal} entities based on the article's {@code eIssn}
   * field. Will set {@code journals} to an empty set if {@code eIssn} is null; otherwise, always expects {@code eIssn}
   * to match to a journal in the system.
   *
   * @param article the article to modify
   * @throws RestClientException if a non-null {@code article.eIssn} isn't matched to a journal in the database
   */
  private void relateToJournals(Article article) {
    /*
     * This sets a maximum of one journal, replicating web Admin behavior.
     * TODO: If an article should have multiple journals, how does it get them?
     */

    String eissn = article.geteIssn();
    Set<Journal> journals;
    if (eissn == null) {
      log.warn("eIssn not set for article");
      journals = Sets.newHashSetWithExpectedSize(0);
    } else {
      Journal journal = (Journal) DataAccessUtils.uniqueResult((List<?>)
          hibernateTemplate.findByCriteria(journalCriteria()
                  .add(Restrictions.eq("eIssn", eissn))
          ));
      if (journal == null) {
        String msg = "XML contained eIssn that was not matched to a journal: " + eissn;
        throw new RestClientException(msg, HttpStatus.BAD_REQUEST);
      }
      journals = Sets.newHashSet(journal);
    }
    article.setJournals(journals);
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
      if (!articleService.isAmendment(article)) {
        terms = taxonomyService.classifyArticle(xml);
        if (terms != null && terms.size() > 0) {
          articleService.setArticleCategories(article, terms);
        } else {
          article.setCategories(new HashMap<Category, Integer>());
        }
      } else {
        article.setCategories(new HashMap<Category, Integer>());
      }
    } catch (TaxonomyClassificationService.TaxonomyClassificationServiceNotConfiguredException e) {
      log.info("Taxonomy server not configured. Ingesting article without categories.");
    } catch (Exception e) {
      log.warn("Taxonomy server not responding, but ingesting article anyway", e);
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
      hibernateTemplate.execute(new HibernateCallback<Integer>() {
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
        throw complainAboutXml(e);
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
      Article otherArticle = (Article) DataAccessUtils.uniqueResult((List<?>) hibernateTemplate.findByCriteria(
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
   * <p/>
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
        hibernateTemplate.delete(previous);
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

  /**
   * Parse an asset from multiple redundant asset nodes only if they have equal values. Throw an exception if two nodes
   * do not describe the same asset.
   *
   * @param assetNodes    one or more article XML nodes representing an asset
   * @param assetIdentity the identity of the asset to parse
   * @return the parsed asset
   * @throws XmlContentException
   */
  private static ArticleAsset parseDistinctAsset(List<Node> assetNodes, AssetIdentity assetIdentity) throws XmlContentException {
    Preconditions.checkArgument(!assetNodes.isEmpty());
    Iterator<Node> nodeIterator = assetNodes.iterator();
    ArticleAsset asset = parseAssetNode(nodeIterator.next(), assetIdentity);
    while (nodeIterator.hasNext()) {
      ArticleAsset nextAsset = parseAssetNode(nodeIterator.next(), assetIdentity);
      if (!haveEqualFields(asset, nextAsset)) {
        String errorMsg = "Article XML contains multiple, non-matching assets with DOI=" + assetIdentity.getIdentifier();
        throw new XmlContentException(errorMsg);
      }
    }
    return asset;
  }

  private static ArticleAsset parseAssetNode(Node assetNode, AssetIdentity assetIdentity) throws XmlContentException {
    return new AssetXml(assetNode, assetIdentity).build(new ArticleAsset());
  }

  /**
   * Check if two assets have equal values for all fields defined in article XML. This is more stringent than {@link
   * ArticleAsset#equals(Object)}.
   *
   * @param a1 an asset
   * @param a2 another asset
   * @return {@code true} they have equal values for all fields defined in article XML
   */
  private static boolean haveEqualFields(ArticleAsset a1, ArticleAsset a2) {
    if (Preconditions.checkNotNull(a1) == Preconditions.checkNotNull(a2)) return true;
    if (!Objects.equal(a1.getDoi(), a2.getDoi())) return false;
    if (!Objects.equal(a1.getContextElement(), a2.getContextElement())) return false;
    if (!Objects.equal(a1.getExtension(), a2.getExtension())) return false;
    if (!Objects.equal(a1.getContentType(), a2.getContentType())) return false;
    if (!Objects.equal(a1.getTitle(), a2.getTitle())) return false;
    if (!Objects.equal(a1.getDescription(), a2.getDescription())) return false;
    return true;
  }

  private static RestClientException complainAboutXml(XmlContentException e) {
    String msg = "Error in submitted XML";
    String nestedMsg = e.getMessage();
    if (StringUtils.isNotBlank(nestedMsg)) {
      msg = msg + " -- " + nestedMsg;
    }
    return new RestClientException(msg, HttpStatus.BAD_REQUEST, e);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream readXml(ArticleIdentity id) {
    try {
      return contentRepoService.getLatestRepoObject(id.forXmlAsset().toString());
    } catch (ContentRepoException e) {
      if (e.getErrorType() == ErrorType.ErrorFetchingObject) {
        throw reportNotFound(id);
      } else {
        throw e;
      }
    }
  }

  @Override
  public Transceiver readMetadata(final DoiBasedIdentity id, final boolean excludeCitations) throws IOException {
    return new EntityTransceiver<Article>() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        Date lastModified = (Date) DataAccessUtils.uniqueResult(hibernateTemplate.find(
            "select lastModified from Article where doi = ?", id.getKey()));
        if (lastModified == null) {
          return null;
        }
        return copyToCalendar(lastModified);
      }

      @Override
      protected Article fetchEntity() {
        Article article = (Article) DataAccessUtils.uniqueResult((List<?>)
            hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
                    .add(Restrictions.eq("doi", id.getKey()))
                    .setFetchMode("categories", FetchMode.SELECT)
                    .setFetchMode("assets", FetchMode.SELECT)
                    .setFetchMode("articleType", FetchMode.JOIN)
                    .setFetchMode("journals", FetchMode.JOIN)
                    .setFetchMode("journals.volumes", FetchMode.JOIN)
                    .setFetchMode("journals.volumes.issues", FetchMode.JOIN)
                    .setFetchMode("journals.articleList", FetchMode.JOIN)
                    .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
            ));
        if (article == null) {
          throw reportNotFound(id);
        }
        return article;
      }

      @Override
      protected Object getView(Article entity) {
        return createArticleView(entity, excludeCitations);
      }
    };
  }

  @Override
  public Transceiver readMetadata(final Article article, final boolean excludeCitations) throws IOException {
    return new EntityTransceiver<Article>() {
      @Override
      protected Article fetchEntity() {
        return article;
      }

      @Override
      protected Object getView(Article entity) {
        return createArticleView(entity, excludeCitations);
      }
    };
  }

  private ArticleOutputView createArticleView(Article article, boolean excludeCitations) {
    return articleOutputViewFactory.create(article, excludeCitations);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Transceiver readAuthors(final ArticleIdentity id)
      throws IOException {
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        AssetFileIdentity xmlAssetIdentity = id.forXmlAsset();
        Date lastModified = (Date) DataAccessUtils.uniqueResult(hibernateTemplate.find(
            "select lastModified from ArticleAsset where doi = ? and extension = ?",
            xmlAssetIdentity.getKey(), xmlAssetIdentity.getFileExtension()));
        if (lastModified == null) {
          throw reportNotFound(id);
        }
        return copyToCalendar(lastModified);
      }

      @Override
      protected Object getData() throws IOException {
        Document doc = parseXml(readXml(id));
        List<AuthorView> authors;
        try {
          authors = AuthorsXmlExtractor.getAuthors(doc, xpathReader);
        } catch (XPathException e) {
          throw new RuntimeException("Invalid XML when parsing authors from: " + id, e);
        }
        return ArticleAuthorView.createList(authors);
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(ArticleIdentity id) {
    Article article = findArticleById(id);
    if (article == null) {
      throw reportNotFound(id);
    }

    for (ArticleAsset asset : article.getAssets()) {
      if (AssetIdentity.hasFile(asset)) {
        AssetFileIdentity assetFileIdentity = AssetFileIdentity.from(asset);
        deleteAssetFile(assetFileIdentity);
      }
    }
    hibernateTemplate.delete(article);
  }

  @Override
  public Transceiver listDois(final ArticleCriteria articleCriteria)
      throws IOException {
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }

      @Override
      protected Object getData() throws IOException {
        return articleCriteria.apply(hibernateTemplate);
      }
    };
  }

  @Override
  public Transceiver listRecent(RecentArticleQuery query)
      throws IOException {
    return query.execute(hibernateTemplate);
  }

  @Override
  public Collection<RelatedArticleView> getRelatedArticles(Article article) {
    List<ArticleRelationship> rawRelationships = article.getRelatedArticles();
    List<RelatedArticleView> relatedArticleViews = Lists.newArrayListWithCapacity(rawRelationships.size());
    for (ArticleRelationship rawRelationship : rawRelationships) {
      if (rawRelationship.getOtherArticleID() == null) { continue; } // ignore when doi not present in article table
      String otherArticleDoi = rawRelationship.getOtherArticleDoi();

      // Simple and inefficient implementation. Same solution as legacy Ambra. TODO: Optimize
      Article relatedArticle = (Article) DataAccessUtils.uniqueResult((List<?>)
          hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
              .add(Restrictions.eq("doi", otherArticleDoi))));

      RelatedArticleView relatedArticleView = new RelatedArticleView(rawRelationship, relatedArticle.getTitle(),
              relatedArticle.getAuthors());
      relatedArticleViews.add(relatedArticleView);
    }
    return relatedArticleViews;
  }

  @Required
  public void setAssetService(AssetCrudService assetService) {
    this.assetService = assetService;
  }
}
