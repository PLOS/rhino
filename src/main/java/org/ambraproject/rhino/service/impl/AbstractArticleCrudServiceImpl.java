/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package org.ambraproject.rhino.service.impl;

import static java.lang.Math.max;
import static java.lang.Math.min;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.xpath.XPathException;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleItemIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleCategoryAssignment;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ArticleRelationship;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.article.RelatedArticleLink;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.CacheableResponse;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.view.ResolvedDoiView;
import org.ambraproject.rhino.view.article.ArticleIngestionView;
import org.ambraproject.rhino.view.article.ArticleOverview;
import org.ambraproject.rhino.view.article.ArticleRevisionView;
import org.ambraproject.rhino.view.article.CategoryAssignmentView;
import org.ambraproject.rhino.view.article.ItemSetView;
import org.ambraproject.rhino.view.article.author.ArticleAllAuthorsView;
import org.ambraproject.rhino.view.article.author.AuthorView;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;

/**
 * Service implementing _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@SuppressWarnings("JpaQlInspection")
public abstract class AbstractArticleCrudServiceImpl extends AmbraService implements ArticleCrudService {

  private static final Logger LOG = LogManager.getLogger(AbstractArticleCrudServiceImpl.class);

  private static final Joiner SPACE_JOINER = Joiner.on(' ');

  public static final int MAX_PAGE_SIZE = 1000;

  @Autowired
  private XpathReader xpathReader;
  @Autowired
  private TaxonomyService taxonomyService;
  @Autowired
  private ArticleIngestionView.Factory articleIngestionViewFactory;
  @Autowired
  private ItemSetView.Factory itemSetViewFactory;
  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  String bucketName() {
    return runtimeConfiguration.getCorpusBucket();
  }

// <<<<<<< HEAD:src/main/java/org/ambraproject/rhino/service/impl/AbstractArticleCrudServiceImpl.java
// =======
//   public Archive repack(ArticleIngestionIdentifier ingestionId) {

//     String bucketName = runtimeConfiguration.getCorpusBucket();
//     Map<String, ByteSource> archiveMap = files.stream().collect(Collectors.toMap(
//         ArticleFile::getIngestedFileName,
//         (ArticleFile file) -> new ByteSource() {
//           @Override
//           public InputStream openStream() throws IOException {
//             return contentRepoService.getRepoObject(file.getCrepoVersion(bucketName));
//           }
//         }));

//     return Archive.pack(extractFilenameStub(ingestionId.getDoiName()) + ".zip", archiveMap);
//   }

//   private static final Pattern FILENAME_STUB_PATTERN = Pattern.compile("(?:[^/]*/)*?([^/]*)/?");

//   private static String extractFilenameStub(String name) {
//     Matcher m = FILENAME_STUB_PATTERN.matcher(name);
//     return m.matches() ? m.group(1) : name;
//   }

//   @Override
// >>>>>>> docker:src/main/java/org/ambraproject/rhino/service/impl/ArticleCrudServiceImpl.java

  @Override
  public Archive repack(ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion ingestion = readIngestion(ingestionId);
    @SuppressWarnings("unchecked")
    List<ArticleFile> files = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM ArticleFile WHERE ingestion = :ingestion");
      query.setParameter("ingestion", ingestion);
      return (List<ArticleFile>) query.list();
    });

    Map<String, ByteSource> archiveMap = files.stream().collect(Collectors.toMap(
        ArticleFile::getIngestedFileName,
        (ArticleFile file) -> new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return getInputStream(file);
          }
        }));

    return Archive.pack(extractFilenameStub(ingestionId.getDoiName()) + ".zip", archiveMap);
  }

  private static final Pattern FILENAME_STUB_PATTERN = Pattern.compile("(?:[^/]*/)*?([^/]*)/?");

  private static String extractFilenameStub(String name) {
    Matcher m = FILENAME_STUB_PATTERN.matcher(name);
    return m.matches() ? m.group(1) : name;
  }

  @Override
  public void populateCategories(ArticleIdentifier articleId) throws IOException {
    Article article = readArticle(articleId);
    ArticleRevision revision = readLatestRevision(article);
    taxonomyService.populateCategories(revision);
  }

  @Override
  public CacheableResponse<ArticleIngestionView> serveMetadata(final ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion ingestion = readIngestion(ingestionId);
    return CacheableResponse.serveEntity(ingestion, articleIngestionViewFactory::getView);
  }

  @Override
  public CacheableResponse<ItemSetView> serveItems(ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion ingestion = readIngestion(ingestionId);
    return CacheableResponse.serveEntity(ingestion, itemSetViewFactory::getView);
  }

  @SuppressWarnings("unchecked")
  @Override
  public ArticleOverview buildOverview(Article article) {
    return hibernateTemplate.execute(session -> {
      Query ingestionQuery = session.createQuery("FROM ArticleIngestion WHERE article = :article");
      ingestionQuery.setParameter("article", article);
      List<ArticleIngestion> ingestions = ingestionQuery.list();

      Query revisionQuery = session.createQuery("" +
          "FROM ArticleRevision WHERE ingestion IN " +
          "  (FROM ArticleIngestion WHERE article = :article)");
      revisionQuery.setParameter("article", article);
      List<ArticleRevision> revisions = revisionQuery.list();

      ArticleIdentifier id = ArticleIdentifier.create(article.getDoi());
      return ArticleOverview.build(id, ingestions, revisions);
    });
  }

  @SuppressWarnings("unchecked")
  @Override
  public ServiceResponse<ArticleOverview> serveOverview(ArticleIdentifier id) {
    ArticleOverview view = hibernateTemplate.execute(session -> {
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
    return ServiceResponse.serveView(view);
  }

  @Override
  public ServiceResponse<List<ArticleRevisionView>> serveRevisions(ArticleIdentifier id) {
    Article article = readArticle(id);
    @SuppressWarnings("unchecked")
    List<ArticleRevision> revisions = (List<ArticleRevision>) hibernateTemplate.find(
        "FROM ArticleRevision WHERE ingestion.article = ? ORDER BY revisionNumber", article);
    List<ArticleRevisionView> views = Lists.transform(revisions, ArticleRevisionView::getView);
    return ServiceResponse.serveView(views);
  }

  @Override
  public CacheableResponse<ArticleRevisionView> serveRevision(ArticleRevisionIdentifier revisionId) {
    ArticleRevision revision = readRevision(revisionId);
    return CacheableResponse.serveEntity(revision, ArticleRevisionView::getView);
  }

  @Override
  public CacheableResponse<ArticleAllAuthorsView> serveAuthors(ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion articleIngestion = readIngestion(ingestionId);
    return CacheableResponse.serveEntity(articleIngestion, ing -> parseAuthors(getManuscriptXml(ing)));
  }

  private ArticleAllAuthorsView parseAuthors(Document doc) {
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
  public ServiceResponse<Collection<CategoryAssignmentView>> serveCategories(final ArticleIdentifier articleId)
      throws IOException {
    Article article = readArticle(articleId);
    Collection<ArticleCategoryAssignment> categoryAssignments = taxonomyService.getAssignmentsForArticle(article);
    Collection<CategoryAssignmentView> views = categoryAssignments.stream()
        .map(CategoryAssignmentView::new)
        .collect(Collectors.toList());
    return ServiceResponse.serveView(views);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceResponse<List<String>> serveRawCategories(final ArticleIdentifier articleId) throws IOException {
    Article article = readArticle(articleId);
    ArticleRevision revision = readLatestRevision(article);
    Document manuscriptXml = getManuscriptXml(revision.getIngestion());
    List<String> rawTerms = taxonomyService.getRawTerms(manuscriptXml, article, false /*isTextRequired*/);
    List<String> cleanedTerms = new ArrayList<>();
    for (String term : rawTerms) {
      term = term.replaceAll("<TERM>", "").replaceAll("</TERM>", "");
      cleanedTerms.add(term);
    }
    return ServiceResponse.serveView(cleanedTerms);
  }

  /**
   * {@inheritDoc}
   *
   * @param articleId
   */
  @Override
  public String getRawCategoriesAndText(final ArticleIdentifier articleId) throws IOException {
    Article article = readArticle(articleId);
    ArticleRevision revision = readLatestRevision(article);
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


  @SuppressWarnings("unchecked")
  @Override
  public List<ArticleRelationship> getRelationshipsFrom(ArticleIdentifier sourceId) {
    return (List<ArticleRelationship>) hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "SELECT ar " +
          "FROM ArticleRelationship ar " +
          "WHERE ar.sourceArticle.doi = :doi ");
      query.setParameter("doi", sourceId.getDoiName());
      return query.list();
    });
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<ArticleRelationship> getRelationshipsTo(ArticleIdentifier targetId) {
    return (List<ArticleRelationship>) hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "SELECT ar " +
          "FROM ArticleRelationship ar " +
          "WHERE ar.targetArticle.doi = :doi ");
      query.setParameter("doi", targetId.getDoiName());
      return query.list();
    });
  }

  @Override
  public void refreshArticleRelationships(ArticleRevision sourceArticleRev) {
    ArticleXml sourceArticleXml = new ArticleXml(getManuscriptXml(sourceArticleRev.getIngestion()));
    Article sourceArticle = sourceArticleRev.getIngestion().getArticle();

    /* Drop old relationships */
    getRelationshipsFrom(ArticleIdentifier.create(sourceArticle.getDoi()))
      .forEach(hibernateTemplate::delete);

    for (RelatedArticleLink ar: sourceArticleXml.parseRelatedArticles()) {
      if (getArticle(ar.getArticleId()).isPresent()) {
        hibernateTemplate.save(fromRelatedArticleLink(sourceArticle, ar));
      }
    }
  }

  @Override
  public ArticleRelationship fromRelatedArticleLink(Article article, RelatedArticleLink ral) {
    ArticleRelationship ar = new ArticleRelationship();
    ar.setSourceArticle(Objects.requireNonNull(article));
    Article targetArticle = getArticle(ral.getArticleId()).orElse(null);
    ar.setTargetArticle(Objects.requireNonNull(targetArticle));
    ar.setType(Objects.requireNonNull(ral.getType()));
    ar.setSpecificUse(ral.getSpecificUse());
    return ar;
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

  @SuppressWarnings("unchecked")
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

  @SuppressWarnings("unchecked")
  @Override
  public Collection<ArticleItem> getAllArticleItems(ArticleIngestion ingestion) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM ArticleItem WHERE ingestion = :ingestion");
      query.setParameter("ingestion", ingestion);
      return (Collection<ArticleItem>) query.list();
    });
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<ResolvedDoiView> getItemOverview(Doi doi) {
    return (Optional<ResolvedDoiView>) hibernateTemplate.execute(session -> {
      Query ingestionQuery = session.createQuery("FROM ArticleItem WHERE doi = :doi");
      ingestionQuery.setParameter("doi", doi.getName());
      List<ArticleItem> items = ingestionQuery.list();
      if (items.isEmpty()) return Optional.empty();

      ResolvedDoiView.DoiWorkType type = items.stream().allMatch(AbstractArticleCrudServiceImpl::isMainArticleItem)
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
  public Optional<ArticleIngestion> getIngestion(ArticleIngestionIdentifier id) {
    return Optional.ofNullable(hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM ArticleIngestion " +
          "WHERE article.doi = :doi " +
          "  AND ingestionNumber = :ingestionNumber");
      query.setParameter("doi", id.getDoiName());
      query.setParameter("ingestionNumber", id.getIngestionNumber());
      return (ArticleIngestion) query.uniqueResult();
    }));
  }

  @Override
  public ArticleIngestion readIngestion(ArticleIngestionIdentifier id) {
    return getIngestion(id).orElseThrow(() ->
        new RestClientException("Ingestion not found: " + id, HttpStatus.NOT_FOUND));
  }

  @Override
  public Optional<ArticleRevision> getRevision(ArticleRevisionIdentifier revisionId) {
    return Optional.ofNullable(hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM ArticleRevision " +
          "WHERE revisionNumber = :revisionNumber " +
          "AND ingestion.article.doi = :doi");
      query.setParameter("revisionNumber", revisionId.getRevision());
      query.setParameter("doi", revisionId.getDoiName());
      return (ArticleRevision) query.uniqueResult();
    }));
  }

  @Override
  public ArticleRevision readRevision(ArticleRevisionIdentifier revisionId) {
    return getRevision(revisionId).orElseThrow(() ->
        new RestClientException("Revision not found: " + revisionId, HttpStatus.NOT_FOUND));
  }

  @Override
  public Optional<ArticleRevision> getLatestRevision(Article article) {
    Integer maxRevisionNumber = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "SELECT MAX(rev.revisionNumber) " +
          "FROM ArticleRevision rev, ArticleItem item " +
          "WHERE item.doi = :doi " +
          "  AND rev.ingestion = item.ingestion");
      query.setParameter("doi", Doi.create(article.getDoi()).getName());
      return (Integer) query.uniqueResult();
    });
    if (maxRevisionNumber == null) return Optional.empty();
    return Optional.ofNullable(hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM ArticleRevision as av " +
          "WHERE av.ingestion.article = :article " +
          "AND av.revisionNumber = :latestRevision"
      );
      query.setParameter("article", article);
      query.setParameter("latestRevision", maxRevisionNumber);
      return (ArticleRevision) query.uniqueResult();
    }));
  }

  @Override
  public ArticleRevision readLatestRevision(Article article) {
    return getLatestRevision(article).orElseThrow(() ->
        new RestClientException("No revisions found for " + article.getDoi(), HttpStatus.NOT_FOUND));
  }

  @Override
  public Optional<Article> getArticle(ArticleIdentifier articleIdentifier) {
    return Optional.ofNullable(hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Article WHERE doi = :doi");
      query.setParameter("doi", articleIdentifier.getDoiName());
      return (Article) query.uniqueResult();
    }));
  }

  @Override
  public Article readArticle(ArticleIdentifier articleIdentifier) {
    return getArticle(articleIdentifier).orElseThrow(() ->
        new RestClientException("Article not found: " + articleIdentifier, HttpStatus.NOT_FOUND));
  }

  @Override
  public Collection<ArticleRevision> getArticlesPublishedOn(LocalDate fromDate, LocalDate toDate) {
    return hibernateTemplate.execute(session -> {
      final String queryString = SPACE_JOINER
        .join(
              "SELECT DISTINCT ar",
              "FROM ArticleRevision ar",
              "INNER JOIN ar.ingestion ai", "INNER JOIN  ar.ingestion.article at",
              "WHERE ai.publicationDate >= :fromDate AND ai.publicationDate <= :toDate",
              "AND ar.revisionId IS NOT NULL");
      Query query = session.createQuery(queryString);
      query.setParameter("fromDate", java.sql.Date.valueOf(fromDate));
      query.setParameter("toDate", java.sql.Date.valueOf(toDate));
      return (Collection<ArticleRevision>) query.list();
    });
  }

  @Override
  public Collection<ArticleRevision> getArticlesRevisedOn(LocalDate fromDate, LocalDate toDate) {
    return hibernateTemplate.execute(session -> {
      final String queryString = SPACE_JOINER
        .join("SELECT DISTINCT ar",
              "FROM ArticleRevision ar",
              "INNER JOIN ar.ingestion ai", "INNER JOIN  ar.ingestion.article at",
              "WHERE ai.revisionDate >= :fromDate AND ai.revisionDate <= :toDate",
              "AND ar.revisionId IS NOT NULL");
      Query query = session.createQuery(queryString);
      query.setParameter("fromDate", java.sql.Date.valueOf(fromDate));
      query.setParameter("toDate", java.sql.Date.valueOf(toDate));
      return (Collection<ArticleRevision>) query.list();
    });
  }

  @Override
  public void updatePreprintDoi(ArticleIngestionIdentifier articleId, String preprintOfDoi) throws IOException {
    final ArticleIngestion articleIngestion = readIngestion(articleId);
    articleIngestion.setPreprintDoi(preprintOfDoi);
    hibernateTemplate.save(articleIngestion);
  }

  @Override
  public Collection<String> getArticleDois(int pageNumber, int pageSize, SortOrder sortOrder) {
    final Collection<String> articleDois = getArticleDoisForDateRange(
        pageNumber, pageSize, sortOrder, Optional.empty() /* fromDate */,
        Optional.empty() /* toDate */);
    return articleDois;
  }

  @Override
  public Collection<String> getArticleDoisForDateRange(
      int pageNumber, int pageSize, SortOrder sortOrder, Optional<LocalDateTime> fromDate,
      Optional<LocalDateTime> toDate) {
    final long totalArticles = hibernateTemplate.execute(session -> {
      final Query query = session.createQuery("select count(*) from Article");
      final Long count = (Long) query.uniqueResult();
      return count;
    });

    if (totalArticles > 0L) {
      pageNumber = max(pageNumber, 1);
      final int maxResults = min(pageSize, MAX_PAGE_SIZE);
      final int firstResult = (pageNumber - 1) * maxResults;

      if (LOG.isDebugEnabled()) {
        LOG.debug("pageNumber: {}, pageSize: {}", pageNumber, pageSize);
        LOG.debug("firstResult: {}, maxResults: {}", firstResult, maxResults);
        LOG.debug("sortOrder: {}", sortOrder);
      }

      if (firstResult < totalArticles) {
        final DetachedCriteria criteria = DetachedCriteria.forClass(Article.class);
        final ProjectionList projections = Projections.projectionList();
        projections.add(Projections.property("doi" /* propertyName */));
        criteria.setProjection(projections);

        // Set restrictions for filtering on date range, if any.
        if (fromDate.isPresent()) {
          criteria.add(Restrictions.ge(
              "created" /* propertyName */, java.sql.Timestamp.valueOf(fromDate.get())));
        }
        if (toDate.isPresent()) {
          criteria.add(Restrictions.le(
              "created" /* propertyName */, java.sql.Timestamp.valueOf(toDate.get())));
        }

        if (sortOrder == SortOrder.OLDEST) {
          criteria.addOrder(Order.asc("created" /* propertyName */));
        } else {
          criteria.addOrder(Order.desc("created" /* propertyName */));
        }

        @SuppressWarnings("unchecked")
        final List<String> articleDois = (List<String>) hibernateTemplate.findByCriteria(
            criteria, firstResult, maxResults);
        return articleDois;
      }
    }
    return ImmutableList.of();
  }

  @Override
  public Document getManuscriptXml(ArticleIngestion ingestion) {
    Doi articleDoi = Doi.create(ingestion.getArticle().getDoi());
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(articleDoi, ingestion.getIngestionNumber());
    ArticleItemIdentifier articleItemId = ingestionId.getItemFor();
    ArticleFileIdentifier manuscriptId = ArticleFileIdentifier.create(articleItemId, "manuscript");
    ArticleFile articleFile = getArticleFile(manuscriptId);
    try (InputStream manuscriptInputStream = getInputStream(articleFile)) {
      return parseXml(manuscriptInputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public ArticleFile getArticleFile(ArticleFileIdentifier fileId) {
    ArticleItemIdentifier id = fileId.getItemIdentifier();
    ArticleItem work = getArticleItem(id);
    String fileType = fileId.getFileType();
    ArticleFile articleFile = work.getFile(fileType)
      .orElseThrow(() -> new RestClientException("Unrecognized type: " + fileType, HttpStatus.NOT_FOUND));
    return articleFile;
  }
}
