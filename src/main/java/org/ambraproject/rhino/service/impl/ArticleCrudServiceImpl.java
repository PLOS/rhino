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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleItemIdentifier;
import org.ambraproject.rhino.identity.ArticleVersionIdentifier;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleAsset;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ArticleRelationship;
import org.ambraproject.rhino.model.Category;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleTypeService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.PingbackReadService;
import org.ambraproject.rhino.service.SyndicationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleAllAuthorsView;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.ambraproject.rhino.view.article.ArticleOutputView;
import org.ambraproject.rhino.view.article.ArticleOutputViewFactory;
import org.ambraproject.rhino.view.article.AuthorView;
import org.ambraproject.rhino.view.article.RelatedArticleView;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.ErrorType;
import org.plos.crepo.model.RepoVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementing _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
public class ArticleCrudServiceImpl extends AmbraService implements ArticleCrudService {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudServiceImpl.class);

  @Autowired
  AssetCrudService assetCrudService;

  @Autowired
  RuntimeConfiguration runtimeConfiguration;
  @Autowired
  protected PingbackReadService pingbackReadService;
  @Autowired
  private ArticleOutputViewFactory articleOutputViewFactory;
  @Autowired
  private XpathReader xpathReader;
  @Autowired
  TaxonomyService taxonomyService;
  @Autowired
  Gson crepoGson;
  @Autowired
  SyndicationService syndicationService;
  @Autowired
  ArticleTypeService articleTypeService;

  private final LegacyIngestionService legacyIngestionService = new LegacyIngestionService(this);
  private final VersionedIngestionService versionedIngestionService = new VersionedIngestionService(this);

  @Override
  public Article findArticleById(DoiBasedIdentity id) {
    return legacyIngestionService.findArticleById(id);
  }

  @Override
  public Article writeArchive(Archive archive, Optional<ArticleIdentity> suppliedId, WriteMode mode, OptionalInt revision) throws IOException {
    Article article;
    if (!runtimeConfiguration.isUsingVersionedIngestion()) {
      article = legacyIngestionService.writeArchive(archive, suppliedId, mode);
    } else {
      try {
        article = versionedIngestionService.ingest(archive, revision);
      } catch (XmlContentException e) {
        throw new RuntimeException(e);
      }
    }

    return article;
  }

  @Override
  public Article writeArchiveAsVersionedOnly(Archive archive) throws IOException {
    if (runtimeConfiguration.isUsingVersionedIngestion()) {
      try {
        return versionedIngestionService.ingest(archive, OptionalInt.empty());
      } catch (XmlContentException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RestClientException("Versioned ingestion is not enabled on this system", HttpStatus.BAD_REQUEST);
    }
  }

  @Override
  public void repopulateCategories(ArticleIdentity id) throws IOException {
    Article article = findArticleById(id);
    Document xml = parseXml(readXml(id));
    legacyIngestionService.populateCategories(article, xml);
  }

  static RestClientException complainAboutXml(XmlContentException e) {
    String msg = "Error in submitted XML";
    String nestedMsg = e.getMessage();
    if (!Strings.isNullOrEmpty(nestedMsg)) {
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
      return contentRepoService.getLatestRepoObject(id.forXmlAsset().getFilePath());
    } catch (ContentRepoException e) {
      if (e.getErrorType() == ErrorType.ErrorFetchingObject) {
        throw reportNotFound(id);
      } else {
        throw e;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Journal getPublicationJournal(Article article) {
    String eissn = article.geteIssn();
    if (eissn == null) {
      String msg = "eIssn not set for article: " + article.getDoi();
      throw new RestClientException(msg, HttpStatus.BAD_REQUEST);
    } else {
      Journal journal = (Journal) DataAccessUtils.uniqueResult((List<?>)
          hibernateTemplate.findByCriteria(journalCriteria().add(Restrictions.eq("eIssn", eissn))));
      if (journal == null) {
        String msg = "XML contained eIssn that was not matched to a journal: " + eissn;
        throw new RestClientException(msg, HttpStatus.BAD_REQUEST);
      }
      return journal;
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

  @Deprecated
  @Override
  public Transceiver readVersionedMetadata(final ArticleVersionIdentifier versionId,
                                           final ArticleMetadataSource source) {
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return copyToCalendar(getArticleItem(versionId.getItemFor()).getTimestamp());
      }

      @Override
      protected Object getData() throws IOException {
        return getView(versionedIngestionService.getArticleMetadata(versionId, source));
      }

      /**
       * Populate fields required by {@link ArticleOutputView} with dummy values. This is a temporary kludge.
       */
      private void kludgeArticleFields(Article article) {
        article.setDoi(versionId.getArticleIdentifier().getDoi().getUri().toString());

        ArticleAsset articleXmlAsset = new ArticleAsset();
        articleXmlAsset.setDoi(article.getDoi());
        articleXmlAsset.setExtension("XML");
        ArticleAsset articlePdfAsset = new ArticleAsset();
        articlePdfAsset.setDoi(article.getDoi());
        articlePdfAsset.setExtension("PDF");
        article.setAssets(ImmutableList.of(articleXmlAsset, articlePdfAsset));

        article.setID(-1L);
        article.setRelatedArticles(ImmutableList.<ArticleRelationship>of());
        article.setJournals(ImmutableSet.<Journal>of());
        article.setCategories(ImmutableMap.<Category, Integer>of());
      }

      private ArticleOutputView getView(Article article) {
        kludgeArticleFields(article);
        boolean excludeCitations = (source == ArticleMetadataSource.FRONT_MATTER);
        return articleOutputViewFactory.create(article, excludeCitations);
      }
    };
  }

  @Override
  public Transceiver readRevisions(ArticleIdentifier id) {
    return new Transceiver() {
      @Override
      protected List<Integer> getData() throws IOException {
        return (List<Integer>) hibernateTemplate.execute(session -> {
          SQLQuery query = session.createSQLQuery("" +
              "SELECT DISTINCT version.revisionNumber " +
              "FROM articleItem item " +
              "INNER JOIN articleVersion version ON item.versionId = version.versionId " +
              "WHERE item.doi = :doi " +
              "ORDER BY version.revisionNumber ASC");
          query.setParameter("doi", id.getDoi().getName());
          return query.list();
        });
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
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
        return ImmutableMap.of();//createArticleView(entity, excludeCitations);
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
        List<String> authorContributions;
        List<String> competingInterests;
        List<String> correspondingAuthorList;
        try {
          authors = AuthorsXmlExtractor.getAuthors(doc, xpathReader);
          authorContributions = AuthorsXmlExtractor.getAuthorContributions(doc, xpathReader);
          competingInterests = AuthorsXmlExtractor.getCompetingInterests(doc, xpathReader);
          correspondingAuthorList = AuthorsXmlExtractor.getCorrespondingAuthorList(doc, xpathReader);
        } catch (XPathException e) {
          throw new RuntimeException("Invalid XML when parsing authors from: " + id, e);
        }
        return new ArticleAllAuthorsView(authors, authorContributions, competingInterests, correspondingAuthorList);
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Transceiver readCategories(final ArticleIdentity id) throws IOException {

    return new EntityTransceiver<Article>() {

      @Override
      protected Article fetchEntity() {
        return findArticleById(id);
      }

      @Override
      protected Object getView(Article entity) {
        return entity.getCategories();
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Transceiver getRawCategories(final ArticleIdentity id)
      throws IOException {

    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() {
        return copyToCalendar(findArticleById(id).getLastModified());
      }

      @Override
      protected Object getData() throws IOException {
        List<String> rawTerms = taxonomyService.getRawTerms(parseXml(readXml(id)),
            findArticleById(id), false);
        List<String> cleanedTerms = new ArrayList<>();
        for (String term : rawTerms) {
          term = term.replaceAll("<TERM>", "").replaceAll("</TERM>", "");
          cleanedTerms.add(term);
        }
        return cleanedTerms;
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getRawCategoriesAndText(final ArticleIdentity id) throws IOException {
    List<String> rawTermsAndText = taxonomyService.getRawTerms(parseXml(readXml(id)),
        findArticleById(id), true);
    StringBuilder cleanedTermsAndText = new StringBuilder();
    cleanedTermsAndText.append("<pre>");
    // HTML-escape the text, which is in the first element of the result array
    cleanedTermsAndText.append(StringEscapeUtils.escapeHtml4(rawTermsAndText.get(0)));
    cleanedTermsAndText.append("\n");

    for (int i = 1; i < rawTermsAndText.size(); i++) {
      String term = rawTermsAndText.get(i).replaceAll("<TERM>", "").replaceAll("</TERM>", "");
      cleanedTermsAndText.append("\n");
      cleanedTermsAndText.append(term);
    }
    cleanedTermsAndText.append("</pre>");
    return cleanedTermsAndText.toString();
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
      AssetFileIdentity assetFileIdentity = AssetFileIdentity.from(asset);
      deleteAssetFile(assetFileIdentity);
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
  public List<RelatedArticleView> getRelatedArticles(Article article) {
    List<ArticleRelationship> rawRelationships = article.getRelatedArticles();
    if (rawRelationships.isEmpty()) return ImmutableList.of();

    Set<String> relatedArticleDois = rawRelationships.stream()
        .map(ArticleRelationship::getOtherArticleDoi)
        .filter(Objects::nonNull) // not every ArticleRelationship points to an article in our own database
        .collect(Collectors.toSet());
    Map<String, Article> relatedArticles = hibernateTemplate.execute(
        (Session session) -> {
          Query query = session.createQuery("FROM Article WHERE doi in :relatedArticleDois");
          query.setParameterList("relatedArticleDois", relatedArticleDois);
          return (List<Article>) query.list();
        })
        .stream().collect(Collectors.toMap(Article::getDoi, Function.identity()));

    // Our Hibernate mappings maintain an explicit order of ArticleRelationships.
    // The Article objects were fetched unordered, so preserve the order of the rawRelationships list.
    return rawRelationships.stream()
        .map((ArticleRelationship relationship) -> {
          Article relatedArticle = relatedArticles.get(relationship.getOtherArticleDoi());
          return new RelatedArticleView(relationship, relatedArticle);
        })
        .collect(Collectors.toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Transceiver readRandom() throws IOException {
    Criteria criteria = hibernateTemplate.getSessionFactory()
        .getCurrentSession().createCriteria(Article.class);

    criteria.add(Restrictions.sqlRestriction("1=1 order by rand()"));
    criteria.setMaxResults(1);

    Object result = criteria.uniqueResult();
    if (result != null) {
      Article randomArticle = (Article) result;
      return readMetadata(randomArticle, true /*excludeCitations*/);
    }
    throw new RestClientException("No articles present in database.", HttpStatus.NOT_FOUND);
  }

  @Override
  public Archive repack(ArticleIdentity articleIdentity) {
    return legacyIngestionService.repack(articleIdentity);
  }

  @Override
  public int getLatestRevision(Doi doi) {
    return hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "SELECT MAX(version.revisionNumber) " +
          "FROM articleItem item " +
          "INNER JOIN articleVersion version ON item.versionId = version.versionId " +
          "WHERE item.doi = :doi");
      query.setParameter("doi", doi.getName());
      Integer maxRevision = (Integer) query.uniqueResult();
      if (maxRevision == null) {
        throw new RestClientException("No revisions found for " + doi.getName(), HttpStatus.NOT_FOUND);
      }
      return maxRevision;
    });
  }

  @Override
  public ArticleItem getArticleItem(ArticleItemIdentifier id) {
    Object[] itemResult = hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "SELECT item.itemId, version.publicationState, item.articleItemType, version.lastModified " +
          "FROM articleItem item " +
          "INNER JOIN articleVersion version ON item.versionId = version.versionId " +
          "WHERE item.doi = :doi AND version.revisionNumber = :revisionNumber " +
          "  AND version.publicationState != :replaced");
      query.setParameter("doi", id.getDoi().getName());
      query.setParameter("revisionNumber", id.getRevision());
      query.setParameter("replaced", ArticleItem.PublicationState.REPLACED.getValue());
      return (Object[]) query.uniqueResult();
    });
    if (itemResult == null) {
      throw new RestClientException("DOI+revision not found: " + id, HttpStatus.NOT_FOUND);
    }
    long itemId = ((Number) itemResult[0]).longValue();
    ArticleItem.PublicationState state = ArticleItem.PublicationState.fromValue((Integer) itemResult[1]);
    String itemType = (String) itemResult[2];
    Date timestamp = (Date) itemResult[3];

    List<Object[]> fileResults = (List<Object[]>) hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "SELECT fileType, crepoKey, crepoUuid " +
          "FROM articleFile " +
          "WHERE itemId = :itemId");
      query.setParameter("itemId", itemId);
      return query.list();
    });
    Map<String, RepoVersion> fileMap = fileResults.stream().collect(Collectors.toMap(
        (Object[] fileResult) -> (String) fileResult[0],
        (Object[] fileResult) -> RepoVersion.create((String) fileResult[1], (String) fileResult[2])));

    return new ArticleItem(DoiBasedIdentity.create(id.getDoi().getName()), itemType, fileMap, id.getRevision(), state, timestamp.toInstant());
  }

}
