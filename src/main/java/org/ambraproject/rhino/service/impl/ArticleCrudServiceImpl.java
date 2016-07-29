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
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleItemIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ArticleRelationship;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.VersionedArticleRelationship;
import org.ambraproject.rhino.model.article.RelatedArticleLink;
import org.ambraproject.rhino.rest.ClientItemId;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.NoSuchArticleIdException;
import org.ambraproject.rhino.service.PingbackReadService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.ResolvedDoiView;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.ambraproject.rhino.view.article.ArticleOutputView;
import org.ambraproject.rhino.view.article.ArticleOutputViewFactory;
import org.ambraproject.rhino.view.article.RelatedArticleView;
import org.ambraproject.rhino.view.article.author.ArticleAllAuthorsView;
import org.ambraproject.rhino.view.article.author.AuthorView;
import org.ambraproject.rhino.view.article.versioned.ArticleIngestionView;
import org.ambraproject.rhino.view.article.versioned.ArticleOverview;
import org.ambraproject.rhino.view.article.versioned.ItemSetView;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.ErrorType;
import org.plos.crepo.model.identity.RepoId;
import org.plos.crepo.model.metadata.RepoObjectMetadata;
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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementing _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@SuppressWarnings("JpaQlInspection")
public class ArticleCrudServiceImpl extends AmbraService implements ArticleCrudService {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudServiceImpl.class);

  @Autowired
  AssetCrudService assetCrudService;
  @Autowired
  protected PingbackReadService pingbackReadService;
  @Autowired
  private ArticleOutputViewFactory articleOutputViewFactory;
  @Autowired
  private XpathReader xpathReader;
  @Autowired
  private TaxonomyService taxonomyService;
  @Autowired
  private VersionedIngestionService versionedIngestionService;
  @Autowired
  private ArticleIngestionView.Factory articleIngestionViewFactory;
  @Autowired
  private ItemSetView.Factory itemSetViewFactory;

  @Override
  public void populateCategories(ArticleIdentifier articleId) throws IOException {
    ArticleTable article = getArticle(articleId);
    ArticleRevision revision = getLatestArticleRevision(article);
    Document manuscriptXml = getManuscriptXml(revision.getIngestion());
    taxonomyService.populateCategories(article, manuscriptXml);
  }

  @Override
  public Document getManuscriptXml(ArticleIngestion ingestion) throws IOException {
    Doi articleDoi = Doi.create(ingestion.getArticle().getDoi());
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(articleDoi, ingestion.getIngestionNumber());
    ArticleItemIdentifier articleItemId = ingestionId.getItemFor();
    ArticleFileIdentifier manuscriptId = ArticleFileIdentifier.create(articleItemId, "manuscript");
    RepoObjectMetadata objectMetadata = assetCrudService.getArticleItemFile(manuscriptId);
    InputStream manuscriptInputStream = contentRepoService.getRepoObject(objectMetadata.getVersion());
    return parseXml(manuscriptInputStream);
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
  public InputStream readXml(ArticleIdentity articleIdentity) {
    RepoId repoId = RepoId.create(runtimeConfiguration.getCorpusStorage().getDefaultBucket(),
        articleIdentity.forXmlAsset().getFilePath());
    try {
      return contentRepoService.getLatestRepoObject(repoId);
    } catch (ContentRepoException e) {
      if (e.getErrorType() == ErrorType.ErrorFetchingObject) {
        throw reportNotFound(articleIdentity);
      } else {
        throw e;
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param article
   */
  @Override
  public Journal getPublicationJournal(ArticleTable article) {
    ArticleRevision revision = getLatestArticleRevision(article);
    Set<Journal> journals = revision.getIngestion().getJournals();
    return journals.iterator().next(); // TODO: Need original journal?
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
  public Transceiver readArticleMetadata(final ArticleIngestionIdentifier ingestionId) {
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return copyToCalendar(getArticleIngestion(ingestionId).getLastModified());
      }

      @Override
      protected Object getData() throws IOException {
        return articleIngestionViewFactory.getView(ingestionId);
      }
    };
  }

  @Override
  public Transceiver readArticleItems(ArticleIngestionIdentifier ingestionId) {
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return copyToCalendar(getArticleIngestion(ingestionId).getLastModified());
      }

      @Override
      protected Object getData() throws IOException {
        return itemSetViewFactory.getView(getArticleIngestion(ingestionId));
      }
    };
  }

  @Override
  public Transceiver readArticleOverview(ArticleIdentifier id) {
    return new Transceiver() {
      @Override
      protected ArticleOverview getData() throws IOException {
        return hibernateTemplate.execute(session -> {
          Query ingestionQuery = session.createQuery("FROM ArticleIngestion WHERE article.doi = :doi");
          ingestionQuery.setParameter("doi", id.getDoiName());
          List<ArticleIngestion> ingestions = ingestionQuery.list();

          Query revisionQuery = session.createQuery("" +
              "FROM ArticleRevision WHERE ingestion IN " +
              "  (FROM ArticleIngestion WHERE article.doi = :doi)");
          revisionQuery.setParameter("doi", id.getDoiName());
          List<ArticleRevision> revisions = revisionQuery.list();

          return ArticleOverview.build(id, ingestions, revisions);
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

  @Override
  public Transceiver readAuthors(ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion articleIngestion = getArticleIngestion(ingestionId);
    return new Transceiver() {

      @Override
      protected Object getData() throws IOException {
        return parseAuthors(getManuscriptXml(articleIngestion));
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return copyToCalendar(articleIngestion.getLastModified());
      }
    };
  }

  private ArticleAllAuthorsView parseAuthors(Document doc) throws IOException {
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
      throw new RuntimeException("Invalid XML when parsing authors", e);
    }
    return new ArticleAllAuthorsView(authors, authorContributions, competingInterests, correspondingAuthorList);
  }

  /**
   * {@inheritDoc}
   *
   * @param articleId
   */
  @Override
  public Transceiver readCategories(final ArticleIdentifier articleId) throws IOException {

    return new EntityTransceiver<ArticleTable>() {

      @Override
      protected ArticleTable fetchEntity() {
        return getArticle(articleId);
      }

      @Override
      protected Object getView(ArticleTable entity) {
        return taxonomyService.getCategoriesForArticle(entity);
      }
    };
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public Transceiver getRawCategories(final ArticleIdentifier articleId) throws IOException {

    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() {
        return null;
      }

      @Override
      protected Object getData() throws IOException {
        ArticleTable article = getArticle(articleId);
        ArticleRevision revision = getLatestArticleRevision(article);
        Document manuscriptXml = getManuscriptXml(revision.getIngestion());
        List<String> rawTerms = taxonomyService.getRawTerms(manuscriptXml, article, false /*isTextRequired*/);
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
   *
   * @param articleId
   */
  @Override
  public String getRawCategoriesAndText(final ArticleIdentifier articleId) throws IOException {
    ArticleTable article = getArticle(articleId);
    ArticleRevision revision = getLatestArticleRevision(article);
    Document manuscriptXml = getManuscriptXml(revision.getIngestion());

    List<String> rawTermsAndText = taxonomyService.getRawTerms(manuscriptXml, article, true /*isTextRequired*/);
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

  @Override
  public String getArticleTitle(ArticleIngestionIdentifier articleIngestionId) {
    return (String) hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
              "SELECT articleTitle " +
              "FROM ArticleIngestion ai " +
              "WHERE ai = :ingestion ");
      query.setParameter("ingestion", articleIngestionId);
      return query.uniqueResult();
    });
  }

  @Override
  public Date getArticlePubDate(ArticleIngestionIdentifier articleIngestionId) {
    return (Date) hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
              "SELECT articlePublicationDate " +
              "FROM ArticleIngestion ai " +
              "WHERE ai = :ingestion ");
      query.setParameter("ingestion", articleIngestionId);
      return query.uniqueResult();
    });
  }

  @Override
  public List<VersionedArticleRelationship> getArticleRelationshipsFrom(ArticleIdentifier sourceId) {
    return (List<VersionedArticleRelationship>) hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "SELECT ar " +
          "FROM VersionedArticleRelationship ar " +
          "WHERE ar.sourceArticle.doi = :doi ");
      query.setParameter("doi", sourceId.getDoiName());
      return query.list();
    });
  }

  @Override
  public List<VersionedArticleRelationship> getArticleRelationshipsTo(ArticleIdentifier targetId) {
    return (List<VersionedArticleRelationship>) hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "SELECT ar " +
          "FROM VersionedArticleRelationship ar " +
          "WHERE ar.targetArticle.doi = :doi ");
      query.setParameter("doi", targetId.getDoiName());
      return query.list();
    });
  }

  @Override
  public void refreshArticleRelationships(ArticleRevisionIdentifier articleRevId) throws IOException {
    ArticleRevision sourceArticleRev = getArticleRevision(articleRevId);
    ArticleXml sourceArticleXml = new ArticleXml(getManuscriptXml(sourceArticleRev.getIngestion()));
    ArticleTable sourceArticle = sourceArticleRev.getIngestion().getArticle();

    List<RelatedArticleLink> xmlRelationships = sourceArticleXml.parseRelatedArticles();
    List<VersionedArticleRelationship> dbRelationships = getArticleRelationshipsFrom(ArticleIdentifier.create(sourceArticle.getDoi()));
    for (VersionedArticleRelationship ar : dbRelationships) {
      hibernateTemplate.delete(ar);
    }
    for (RelatedArticleLink ar : xmlRelationships) {
      ArticleTable targetArticle = null;
      try {
        targetArticle = getArticle(ar.getArticleId());
      } catch (NoSuchArticleIdException e) {
        // likely a reference to an article external to our system and so the relationship is not persisted
      }
      if (targetArticle != null) {
        VersionedArticleRelationship newAr = new VersionedArticleRelationship();
        newAr.setSourceArticle(sourceArticle);
        newAr.setTargetArticle(targetArticle);
        newAr.setType(ar.getType());
        hibernateTemplate.save(newAr);
      }
    }
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
    return versionedIngestionService.repack(articleIdentity);
  }

  @Override
  public int getLatestRevision(Doi doi) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "SELECT MAX(rev.revisionNumber) " +
          "FROM ArticleRevision rev, ArticleItem item " +
          "WHERE item.doi = :doi " +
          "  AND rev.ingestion = item.ingestion");
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
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM ArticleItem " +
          "WHERE doi = :doi " +
          "  AND ingestion.ingestionNumber = :ingestionNumber");
      query.setParameter("doi", id.getDoiName());
      query.setParameter("ingestionNumber", id.getIngestionNumber());
      return (ArticleItem) query.uniqueResult();
    });
  }

  @Override
  public Collection<ArticleItem> getAllArticleItems(Doi doi) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM ArticleItem WHERE doi = :doi ");
      query.setParameter("doi", doi.getName());
      return (Collection<ArticleItem>) query.list();
    });
  }

  private static boolean isMainArticleItem(ArticleItem item) {
    return item.getDoi().equals(item.getIngestion().getArticle().getDoi());
  }

  @Override
  public Optional<ResolvedDoiView> getItemOverview(Doi doi) {
    return hibernateTemplate.execute(session -> {
      Query ingestionQuery = session.createQuery("FROM ArticleItem WHERE doi = :doi");
      ingestionQuery.setParameter("doi", doi.getName());
      List<ArticleItem> items = ingestionQuery.list();
      if (items.isEmpty()) return Optional.empty();

      ResolvedDoiView.DoiWorkType type = items.stream().allMatch(ArticleCrudServiceImpl::isMainArticleItem)
          ? ResolvedDoiView.DoiWorkType.ARTICLE : ResolvedDoiView.DoiWorkType.ASSET;
      ArticleIdentifier articleId = Iterables.getOnlyElement(items.stream()
          .map(item -> ArticleIdentifier.create(item.getIngestion().getArticle().getDoi()))
          .collect(Collectors.toSet()));

      Query revisionQuery = session.createQuery("" +
          "FROM ArticleRevision WHERE ingestion IN " +
          "  (SELECT ingestion FROM ArticleItem WHERE doi = :doi)");
      revisionQuery.setParameter("doi", doi.getName());
      List<ArticleRevision> revisions = revisionQuery.list();

      Collection<ArticleIngestion> ingestions = Collections2.transform(items, ArticleItem::getIngestion);
      ArticleOverview articleOverview = ArticleOverview.build(articleId, ingestions, revisions);
      return Optional.of(ResolvedDoiView.createForArticle(doi, type, articleOverview));
    });
  }

  @Override
  public ArticleIngestion getArticleIngestion(ArticleIngestionIdentifier id) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM ArticleIngestion " +
          "WHERE article.doi = :doi " +
          "  AND ingestionNumber = :ingestionNumber");
      query.setParameter("doi", id.getDoiName());
      query.setParameter("ingestionNumber", id.getIngestionNumber());
      return (ArticleIngestion) query.uniqueResult();
    });
  }

  @Override
  public ArticleRevision getArticleRevision(ArticleRevisionIdentifier revisionId) {
    ArticleRevision revision = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM ArticleRevision " +
          "WHERE revisionNumber = :revisionNumber " +
          "AND ingestion.article.doi = :doi");
      query.setParameter("revisionNumber", revisionId.getRevision());
      query.setParameter("doi", revisionId.getDoiName());
      return (ArticleRevision) query.uniqueResult();
    });
    if (revision == null) {
      throw new NoSuchArticleIdException(revisionId);
    }
    return revision;
  }

  @Override
  public ArticleRevision getArticleRevision(ArticleIngestionIdentifier articleIngestionId) {
    ArticleRevision revision = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM ArticleRevision " +
          "WHERE ingestion = :articleIngestion");
      query.setParameter("articleIngestion", getArticleIngestion(articleIngestionId));
      return (ArticleRevision) query.uniqueResult();
    });
    if (revision == null) {
      throw new NoSuchArticleIdException(articleIngestionId);
    }
    return revision;
  }

  @Override

  public ArticleRevision getLatestArticleRevision(ArticleTable article) {
    int latestRevision = getLatestRevision(Doi.create(article.getDoi()));
    ArticleRevision revision = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM ArticleRevision as av " +
          "WHERE av.ingestion.article = :article " +
          "AND av.revisionNumber = :latestRevision"
      );
      query.setParameter("article", article);
      query.setParameter("latestRevision", latestRevision);
      return (ArticleRevision) query.uniqueResult();
    });
    if (revision == null) {
      throw new NoSuchArticleIdException(ArticleIdentifier.create(article.getDoi()));
    }
    return revision;
  }

  @Override
  public ArticleTable getArticle(ArticleIdentifier articleIdentifier) {
    ArticleTable article = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM ArticleTable WHERE doi = :doi");
      query.setParameter("doi", articleIdentifier.getDoiName());
      return (ArticleTable) query.uniqueResult();
    });
    if (article == null) {
      throw new NoSuchArticleIdException(articleIdentifier);
    }
    return article;
  }

  private ArticleIngestionIdentifier resolveRevisionToIngestion(Doi doi, int revisionNumber) {
    Integer ingestionNumber = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "SELECT ingestion.ingestionNumber " +
          "FROM ArticleRevision " +
          "WHERE revisionNumber = :revisionNumber " +
          "  AND ingestion.article.doi = :doi");
      query.setParameter("doi", doi.getName());
      query.setParameter("revisionNumber", revisionNumber);
      return (Integer) query.uniqueResult();
    });
    if (ingestionNumber == null) {
      throw new RestClientException(String.format("Revision %d not found for: %s", revisionNumber, doi.getName()),
          HttpStatus.NOT_FOUND);
    }
    return ArticleIngestionIdentifier.create(doi, ingestionNumber);
  }

  @Override
  public ArticleIngestionIdentifier resolveToIngestion(ClientItemId id) {
    Doi doi = id.getDoi();
    ClientItemId.NumberType type = id.getNumberType();
    if (type == null) {
      return resolveRevisionToIngestion(doi, getLatestRevision(doi));
    } else if (type == ClientItemId.NumberType.INGESTION) {
      return ArticleIngestionIdentifier.create(doi, id.getNumber());
    } else if (type == ClientItemId.NumberType.REVISION) {
      return resolveRevisionToIngestion(doi, id.getNumber());
    } else {
      throw new AssertionError();
    }
  }

  private ArticleItemIdentifier resolveRevisionToItem(Doi doi, int revisionNumber) {
    Integer ingestionNumber = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "SELECT item.ingestion.ingestionNumber " +
          "FROM ArticleItem item, ArticleRevision rev " +
          "WHERE rev.revisionNumber = :revisionNumber " +
          "  AND item.doi = :doi " +
          "  AND item.ingestion = rev.ingestion");
      query.setParameter("doi", doi.getName());
      query.setParameter("revisionNumber", revisionNumber);
      return (Integer) query.uniqueResult();
    });
    if (ingestionNumber == null) {
      throw new RestClientException(String.format("Revision %d not found for: %s", revisionNumber, doi.getName()),
          HttpStatus.NOT_FOUND);
    }
    return ArticleItemIdentifier.create(doi, ingestionNumber);
  }

  @Override
  public ArticleItemIdentifier resolveToItem(ClientItemId id) {
    Doi doi = id.getDoi();
    ClientItemId.NumberType type = id.getNumberType();
    if (type == null) {
      return resolveRevisionToItem(doi, getLatestRevision(doi));
    } else if (type == ClientItemId.NumberType.INGESTION) {
      return ArticleItemIdentifier.create(doi, id.getNumber());
    } else if (type == ClientItemId.NumberType.REVISION) {
      return resolveRevisionToItem(doi, id.getNumber());
    } else {
      throw new AssertionError();
    }
  }

}
