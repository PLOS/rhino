/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.inject.internal.Preconditions;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.ArticleRelationship;
import org.ambraproject.models.Category;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.AssetNode;
import org.ambraproject.rhino.content.xml.AssetXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Service implementing _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
public class ArticleCrudServiceImpl extends AmbraService implements ArticleCrudService {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudServiceImpl.class);

  private AssetCrudService assetService;

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
  private Article findArticleById(DoiBasedIdentity id) {
    return (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", id.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Article write(InputStream file, Optional<ArticleIdentity> suppliedId, WriteMode mode) throws IOException, FileStoreException {
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
    String fsid = doi.forXmlAsset().getFsid(); // do this first, to fail fast if the DOI is invalid

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
    initializeAssets(article, xml, xmlDataLength);
    populateRelatedArticles(article);

    return article;
  }

  /**
   * Saves both the hibernate entity and the filestore bytes representing an article.
   *
   * @param article the new or updated Article instance to save
   * @param xmlData bytes of the article XML file to save
   * @return FSID (filestore ID) that the xmlData was saved to
   * @throws IOException
   * @throws FileStoreException
   */
  private String persistArticle(Article article, byte[] xmlData)
      throws IOException, FileStoreException {
    if (article.getID() == null) {
      hibernateTemplate.save(article);
    } else {
      hibernateTemplate.update(article);
    }
    String doi = article.getDoi();

    // ArticleIdentity doesn't like this part of the DOI.
    doi = doi.substring("info:doi/".length());
    String fsid = ArticleIdentity.create(doi).forXmlAsset().getFsid();
    write(xmlData, fsid);
    return fsid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Article writeArchive(String filename, Optional<ArticleIdentity> suppliedId,
                              WriteMode mode) throws IOException, FileStoreException {
    ZipFile zip = new ZipFile(filename);
    Document manifestDoc = parseXml(readZipFile(zip, "MANIFEST.xml"));
    ManifestXml manifest = new ManifestXml(manifestDoc);
    byte[] xmlData = readZipFile(zip, manifest.getArticleXml());
    Document doc = parseXml(xmlData);
    Article article = populateArticleFromXml(doc, suppliedId, mode, xmlData.length);
    article.setArchiveName(new File(filename).getName());
    article.setStrkImgURI(manifest.getStrkImgURI());

    // Save now, before we add asset files, since AssetCrudServiceImpl will expect the
    // Article to be persisted at this point.
    persistArticle(article, xmlData);
    addAssetFiles(article, zip);
    return article;
  }

  private static final Pattern ZIP_ENTRY_EXCLUDE_RE = Pattern.compile("\\.xml(\\.\\w+)?");

  /**
   * Determines if we should save a file contained within an article archive as an asset.
   *
   * @param filename the name of a file within a .zip archive
   * @return true if this file should be persisted as an asset
   */
  @VisibleForTesting
  public static boolean shouldSaveAssetFile(String filename) {
    Preconditions.checkNotNull(filename);
    filename = filename.toLowerCase().trim();
    if (filename.startsWith("manifest.")) {
      return false;
    }
    Matcher matcher = ZIP_ENTRY_EXCLUDE_RE.matcher(filename);
    return !matcher.find();
  }

  private void addAssetFiles(Article article, ZipFile zipFile)
      throws IOException, FileStoreException {

    // TODO: remove existing files if this is a reingest (see IngesterImpl.java line 324)

    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      if (shouldSaveAssetFile(entry.getName())) {
        String[] fields = entry.getName().split("\\.");

        // Not sure why, but the existing admin code always converts the extension to UPPER.
        String extension = fields[fields.length - 1].toUpperCase();
        String doi = "info:doi/10.1371/journal."
            + entry.getName().substring(0, entry.getName().lastIndexOf('.'));
        InputStream is = zipFile.getInputStream(entry);
        boolean threw = true;
        try {
          assetService.upload(is, AssetFileIdentity.create(doi, extension));
          threw = false;
        } finally {
          Closeables.close(is, threw);
        }
      }
    }
  }

  /**
   * Reads and returns the contents of a file in a .zip archive.
   *
   * @param zipFile  zip file
   * @param filename name of the file within the archive to read
   * @return contents of the file
   * @throws IOException
   */
  private byte[] readZipFile(ZipFile zipFile, String filename) throws IOException {
    InputStream is = zipFile.getInputStream(zipFile.getEntry(filename));
    byte[] bytes;
    boolean threw = true;
    try {
      bytes = readClientInput(is);
      threw = false;
    } finally {
      Closeables.close(is, threw);
    }
    return bytes;
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
          hibernateTemplate.findByCriteria(DetachedCriteria
              .forClass(Journal.class)
              .add(Restrictions.eq("eIssn", eissn))
              .setFetchMode("volumes", FetchMode.JOIN) // so the article can be dumped to JSON later
              .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)));
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

    // Attempt to assign categories to the article based on the taxonomy server.  However,
    // we still want to ingest the article even if this process fails.
    List<String> terms = null;
    try {
      terms = articleClassifier.classifyArticle(xml);
    } catch (Exception e) {
      log.warn("Taxonomy server not responding, but ingesting article anyway", e);
    }
    if (terms != null && terms.size() > 0) {
      articleService.setArticleCategories(article, terms);
    } else {
      article.setCategories(new HashSet<Category>());
    }
  }

  private void initializeAssets(final Article article, ArticleXml xml, int xmlDataLength) {
    List<AssetNode> assetNodes = xml.findAllAssetNodes();
    List<ArticleAsset> assets = article.getAssets();
    if (assets == null) {  // create
      assets = Lists.newArrayListWithCapacity(assetNodes.size());
      article.setAssets(assets);
    } else {  // update

      // Ugly hack copied from the old admin code.  The problem is that hibernate, when it
      // eventually commits this transaction, will insert new articleAsset rows before it
      // deletes the old ones, leading to a MySQL unique constraint violation.  I've tried
      // many, many variations of doing this in hibernate only, without falling back to
      // JDBC, involving flushing and clearing the session in various orders.  However
      // they all lead to either unique constraint violations or optimistic locking
      // exceptions.
      hibernateTemplate.execute(new HibernateCallback() {
        @Override
        public Object doInHibernate(Session session) throws HibernateException, SQLException {
          session.createSQLQuery(
              "update articleAsset " +
                  "set doi = concat('old-', doi), " +
                  "extension = concat('old-', extension) " +
                  "where articleID = :articleID"
          ).setParameter("articleID", article.getID())
              .executeUpdate();
          return null;
        }
      });
      assets.clear();
    }
    for (AssetNode assetNode : assetNodes) {
      ArticleAsset asset = new ArticleAsset();
      writeAsset(asset, assetNode);
      assets.add(asset);
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

  private void populateRelatedArticles(Article article) {
    for (ArticleRelationship relationship : article.getRelatedArticles()) {
      relationship.setParentArticle(article);

      String otherArticleDoi = relationship.getOtherArticleDoi();
      Article otherArticle = (Article) DataAccessUtils.uniqueResult((List<?>) hibernateTemplate.findByCriteria(
          DetachedCriteria.forClass(Article.class)
              .add(Restrictions.eq("doi", otherArticleDoi))
      ));
      if (otherArticle != null) {
        relationship.setOtherArticleID(otherArticle.getID());
      }
      /*
       * The article not being in our system is documented as a valid use case (see Javadoc on ArticleRelationship).
       * TODO: Do we want to send a warning to the client just in case?
       */
    }
  }

  /**
   * Copy metadata into an asset entity from an XML node describing the asset.
   *
   * @param asset     the persistent asset entity to modify
   * @param assetNode the XML node from which to extract data
   * @return the modified asset (same identity)
   */
  private static ArticleAsset writeAsset(ArticleAsset asset, AssetNode assetNode) {
    String assetDoi = assetNode.getDoi();
    AssetIdentity assetIdentity = AssetIdentity.create(assetDoi);

    try {
      return new AssetXml(assetNode.getNode(), assetIdentity).build(asset);
    } catch (XmlContentException e) {
      throw complainAboutXml(e);
    }
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
  public InputStream readXml(ArticleIdentity id) throws FileStoreException {
    if (!articleExistsAt(id)) {
      throw reportNotFound(id);
    }

    // TODO Can an invalid request cause this to throw FileStoreException? If so, wrap in RestClientException.
    return fileStoreService.getFileInStream(id.forXmlAsset().getFsid());
  }

  @Override
  public void readMetadata(HttpServletResponse response, DoiBasedIdentity id, MetadataFormat format) throws IOException {
    assert format == MetadataFormat.JSON;
    Article article = (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
            .add(Restrictions.eq("doi", id.getKey()))
            .setFetchMode("assets", FetchMode.JOIN)
            .setFetchMode("journals", FetchMode.JOIN)
            .setFetchMode("journals.volumes", FetchMode.JOIN)
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (article == null) {
      throw reportNotFound(id);
    }
    readMetadata(response, article, format);
  }

  @Override
  public void readMetadata(HttpServletResponse response, Article article, MetadataFormat format) throws IOException {
    assert format == MetadataFormat.JSON;
    writeJsonToResponse(response, article);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(ArticleIdentity id) throws FileStoreException {
    Article article = findArticleById(id);
    if (article == null) {
      throw reportNotFound(id);
    }
    String fsid = id.forXmlAsset().getFsid(); // make sure we get a valid FSID, as an additional check before deleting anything

    for (ArticleAsset asset : article.getAssets()) {
      if (AssetIdentity.hasFile(asset)) {
        String assetFsid = AssetFileIdentity.from(asset).getFsid();
        fileStoreService.deleteFile(assetFsid);
      }
    }
    fileStoreService.deleteFile(fsid);
    hibernateTemplate.delete(article);
  }

  @Override
  public void listDois(HttpServletResponse response, MetadataFormat format) throws IOException {
    assert format == MetadataFormat.JSON;
    List<?> dois = hibernateTemplate.findByCriteria(DetachedCriteria
        .forClass(Article.class)
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        .setProjection(Projections.property("doi"))
        .addOrder(Order.asc("lastModified"))
    );
    writeJsonToResponse(response, dois);
  }

  @Required
  public void setAssetService(AssetCrudService assetService) {
    this.assetService = assetService;
  }
}
