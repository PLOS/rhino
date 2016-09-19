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
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleItemIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ArticleCategoryAssignment;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.VersionedArticleRelationship;
import org.ambraproject.rhino.model.article.RelatedArticleLink;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.ResolvedDoiView;
import org.ambraproject.rhino.view.article.author.ArticleAllAuthorsView;
import org.ambraproject.rhino.view.article.author.AuthorView;
import org.ambraproject.rhino.view.article.ArticleIngestionView;
import org.ambraproject.rhino.view.article.ArticleOverview;
import org.ambraproject.rhino.view.article.ArticleRevisionView;
import org.ambraproject.rhino.view.article.CategoryAssignmentView;
import org.ambraproject.rhino.view.article.ItemSetView;
import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.Query;
import org.plos.crepo.model.identity.RepoVersion;
import org.plos.crepo.model.metadata.RepoObjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private XpathReader xpathReader;
  @Autowired
  private TaxonomyService taxonomyService;
  @Autowired
  private ArticleIngestionView.Factory articleIngestionViewFactory;
  @Autowired
  private ItemSetView.Factory itemSetViewFactory;

  @Override
  public void populateCategories(ArticleIdentifier articleId) throws IOException {
    ArticleTable article = readArticle(articleId);
    ArticleRevision revision = readLatestRevision(article);
    taxonomyService.populateCategories(revision);
  }

  @Override
  public Document getManuscriptXml(ArticleIngestion ingestion) {
    Doi articleDoi = Doi.create(ingestion.getArticle().getDoi());
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(articleDoi, ingestion.getIngestionNumber());
    ArticleItemIdentifier articleItemId = ingestionId.getItemFor();
    ArticleFileIdentifier manuscriptId = ArticleFileIdentifier.create(articleItemId, "manuscript");
    RepoObjectMetadata objectMetadata = assetCrudService.getArticleItemFile(manuscriptId);
    try (InputStream manuscriptInputStream = contentRepoService.getRepoObject(objectMetadata.getVersion())) {
      return parseXml(manuscriptInputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static RestClientException complainAboutXml(XmlContentException e) {
    String msg = "Error in submitted XML";
    String nestedMsg = e.getMessage();
    if (!Strings.isNullOrEmpty(nestedMsg)) {
      msg = msg + " -- " + nestedMsg;
    }
    return new RestClientException(msg, HttpStatus.BAD_REQUEST, e);
  }

  @Override
  public Transceiver serveMetadata(final ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion ingestion = readIngestion(ingestionId);
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return copyToCalendar(ingestion.getLastModified());
      }

      @Override
      protected Object getData() throws IOException {
        return articleIngestionViewFactory.getView(ingestionId);
      }
    };
  }

  @Override
  public Transceiver serveItems(ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion ingestion = readIngestion(ingestionId);
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return copyToCalendar(ingestion.getLastModified());
      }

      @Override
      protected Object getData() throws IOException {
        return itemSetViewFactory.getView(ingestion);
      }
    };
  }

  @Override
  public Transceiver serveOverview(ArticleIdentifier id) {
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
  public Transceiver serveRevision(ArticleRevisionIdentifier revisionId) {
    return new EntityTransceiver<ArticleRevision>() {
      @Override
      protected ArticleRevision fetchEntity() {
        return readRevision(revisionId);
      }

      @Override
      protected Object getView(ArticleRevision entity) {
        return ArticleRevisionView.getView(entity);
      }
    };
  }

  @Override
  public Transceiver serveAuthors(ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion articleIngestion = readIngestion(ingestionId);
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
  public Transceiver serveCategories(final ArticleIdentifier articleId) throws IOException {
    ArticleTable article = readArticle(articleId);
    return new Transceiver() {
      @Override
      protected Collection<CategoryAssignmentView> getData() throws IOException {
        Collection<ArticleCategoryAssignment> categoryAssignments = taxonomyService.getCategoriesForArticle(article);
        return categoryAssignments.stream().map(CategoryAssignmentView::new).collect(Collectors.toList());
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        // Category changes are not necessarily reflected in article's last modified date.
        // TODO: Check max timestamp among category objects?
        return null;
      }
    };
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public Transceiver serveRawCategories(final ArticleIdentifier articleId) throws IOException {

    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() {
        return null;
      }

      @Override
      protected Object getData() throws IOException {
        ArticleTable article = readArticle(articleId);
        ArticleRevision revision = readLatestRevision(article);
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
    ArticleTable article = readArticle(articleId);
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


  @Override
  public List<VersionedArticleRelationship> getRelationshipsFrom(ArticleIdentifier sourceId) {
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
  public List<VersionedArticleRelationship> getRelationshipsTo(ArticleIdentifier targetId) {
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
  public void refreshArticleRelationships(ArticleRevision sourceArticleRev) {
    ArticleXml sourceArticleXml = new ArticleXml(getManuscriptXml(sourceArticleRev.getIngestion()));
    ArticleTable sourceArticle = sourceArticleRev.getIngestion().getArticle();

    List<RelatedArticleLink> xmlRelationships = sourceArticleXml.parseRelatedArticles();
    List<VersionedArticleRelationship> dbRelationships = getRelationshipsFrom(ArticleIdentifier.create(sourceArticle.getDoi()));
    for (VersionedArticleRelationship ar : dbRelationships) {
      hibernateTemplate.delete(ar);
    }
    for (RelatedArticleLink ar : xmlRelationships) {
      getArticle(ar.getArticleId()).ifPresent((ArticleTable targetArticle) -> {
        VersionedArticleRelationship newAr = new VersionedArticleRelationship();
        newAr.setSourceArticle(sourceArticle);
        newAr.setTargetArticle(targetArticle);
        newAr.setType(ar.getType());
        hibernateTemplate.save(newAr);
      });
      // else, likely a reference to an article external to our system and so the relationship is not persisted
    }
  }

  @Override
  public Archive repack(ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion ingestion = readIngestion(ingestionId);
    List<ArticleFile> files = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM ArticleFile WHERE ingestion = :ingestion");
      query.setParameter("ingestion", ingestion);
      return (List<ArticleFile>) query.list();
    });

    Map<String, ByteSource> archiveMap = files.stream()
        .map(ArticleFile::getCrepoVersion)
        .collect(Collectors.toMap(
            // File name within zip archive
            (RepoVersion v) -> {
              RepoObjectMetadata metadata = contentRepoService.getRepoObjectMetadata(v);
              return metadata.getDownloadName().orElseGet(() -> extractFilenameStub(v.getId().getKey()));
            },

            // File content within zip archive
            (RepoVersion v) -> new ByteSource() {
              @Override
              public InputStream openStream() throws IOException {
                return contentRepoService.getRepoObject(v);
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
  public Collection<ArticleItem> getAllArticleItems(ArticleIngestion ingestion) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM ArticleItem WHERE ingestion = :ingestion");
      query.setParameter("ingestion", ingestion);
      return (Collection<ArticleItem>) query.list();
    });
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
  public Optional<ArticleRevision> getLatestRevision(ArticleTable article) {
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
  public ArticleRevision readLatestRevision(ArticleTable article) {
    return getLatestRevision(article).orElseThrow(() ->
        new RestClientException("No revisions found for " + article.getDoi(), HttpStatus.NOT_FOUND));
  }

  @Override
  public Optional<ArticleTable> getArticle(ArticleIdentifier articleIdentifier) {
    return Optional.ofNullable(hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM ArticleTable WHERE doi = :doi");
      query.setParameter("doi", articleIdentifier.getDoiName());
      return (ArticleTable) query.uniqueResult();
    }));
  }

  @Override
  public ArticleTable readArticle(ArticleIdentifier articleIdentifier) {
    return getArticle(articleIdentifier).orElseThrow(() ->
        new RestClientException("Article not found: " + articleIdentifier, HttpStatus.NOT_FOUND));
  }

  @Override
  public Collection<ArticleRevision> getArticlesPublishedOn(LocalDate fromDate, LocalDate toDate) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "SELECT ar " +
          "FROM ArticleRevision ar " +
          "INNER JOIN ar.ingestion ai " +
          "INNER JOIN  ar.ingestion.article at " +
          "WHERE ai.publicationDate >= :fromDate AND ai.publicationDate <= :toDate " +
          "AND ar.revisionId IS NOT NULL");
      query.setParameter("fromDate", java.sql.Date.valueOf(fromDate));
      query.setParameter("toDate", java.sql.Date.valueOf(toDate));
      return (Collection<ArticleRevision>) query.list();
    });
  }

  @Override
  public Collection<ArticleRevision> getArticlesRevisedOn(LocalDate fromDate, LocalDate toDate) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "SELECT ar " +
          "FROM ArticleRevision ar " +
          "INNER JOIN ar.ingestion ai " +
          "INNER JOIN  ar.ingestion.article at " +
          "WHERE ai.revisionDate >= :fromDate AND ai.revisionDate <= :toDate " +
          "AND ar.revisionId IS NOT NULL");
      query.setParameter("fromDate", java.sql.Date.valueOf(fromDate));
      query.setParameter("toDate", java.sql.Date.valueOf(toDate));
      return (Collection<ArticleRevision>) query.list();
    });
  }

}
