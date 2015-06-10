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
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.ArticleRelationship;
import org.ambraproject.models.Journal;
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
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleAuthorView;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.ambraproject.rhino.view.article.ArticleOutputView;
import org.ambraproject.rhino.view.article.ArticleOutputViewFactory;
import org.ambraproject.rhino.view.article.RelatedArticleView;
import org.ambraproject.views.AuthorView;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
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
import org.w3c.dom.Document;

import javax.xml.xpath.XPathException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Service implementing _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
public class ArticleCrudServiceImpl extends AmbraService implements ArticleCrudService {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudServiceImpl.class);

  AssetCrudService assetService;

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

  private final LegacyIngestionService legacyIngestionService = new LegacyIngestionService(this);
  private final VersionedIngestionService versionedIngestionService = new VersionedIngestionService(this);

  private boolean articleExistsAt(DoiBasedIdentity id) {
    DetachedCriteria criteria = DetachedCriteria.forClass(Article.class)
        .add(Restrictions.eq("doi", id.getKey()));
    return exists(criteria);
  }

  @Override
  public Article findArticleById(DoiBasedIdentity id) {
    return legacyIngestionService.findArticleById(id);
  }

  @Override
  public Article writeArchive(Archive archive, Optional<ArticleIdentity> suppliedId, WriteMode mode) throws IOException {
    Article article = legacyIngestionService.writeArchive(archive, suppliedId, mode);

    try {
      versionedIngestionService.ingest(archive);
    } catch (XmlContentException e) {
      throw new RuntimeException(e);
    }

    return article;
  }

  @VisibleForTesting
  public static boolean shouldSaveAssetFile(String filename, String articleXmlFilename) {
    return LegacyIngestionService.shouldSaveAssetFile(filename, articleXmlFilename);
  }

  @Override
  public void repopulateCategories(ArticleIdentity id) throws IOException {
    legacyIngestionService.repopulateCategories(id);
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
            findArticleById(id));
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
      if (rawRelationship.getOtherArticleID() == null) {
        continue;
      } // ignore when doi not present in article table
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
