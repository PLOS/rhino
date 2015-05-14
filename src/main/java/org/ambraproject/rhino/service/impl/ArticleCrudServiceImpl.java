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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.ArticleRelationship;
import org.ambraproject.models.Category;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.identity.ArticleIdentity;
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
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.ErrorType;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.model.RepoVersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

  private RepoCollectionMetadata fetchArticleCollection(ArticleIdentity id) {
    String identifier = id.getIdentifier();
    Optional<Integer> versionNumber = id.getVersionNumber();
    RepoCollectionMetadata collection;
    if (versionNumber.isPresent()) {
      collection = contentRepoService.getCollection(new RepoVersionNumber(identifier, versionNumber.get()));
    } else {
      collection = contentRepoService.getLatestCollection(identifier);
    }
    return collection;
  }

  private Document fetchArticleXml(ArticleIdentity id) {
    RepoCollectionMetadata collection = fetchArticleCollection(id);

    Map<String, Object> userMetadata = (Map<String, Object>) collection.getJsonUserMetadata().get();
    Map<String, String> manuscriptId = (Map<String, String>) userMetadata.get("manuscript");
    RepoVersion manuscript = RepoVersion.create(manuscriptId.get("key"), manuscriptId.get("uuid"));

    Document document;
    try (InputStream manuscriptStream = contentRepoService.getRepoObject(manuscript)) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder(); // TODO: Efficiency
      document = documentBuilder.parse(manuscriptStream);
    } catch (IOException | SAXException | ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
    return document;
  }

  /**
   * Query for an article by its identifier.
   *
   * @param id the article's identity
   * @return the article, or {@code null} if not found
   */
  @Override
  public Article findArticleById(ArticleIdentity id) {
    Document document = fetchArticleXml(id);
    Article article;
    try {
      article = new ArticleXml(document).build(new Article());
    } catch (XmlContentException e) {
      throw new RuntimeException(e);
    }

    return kludgeArticleFields(article);
  }

  /*
   * While versioned articles from CRepo back end are under development, fill in dummy values for fields not populated
   * by ArticleXml.
   *
   * TODO: Fill in real values or obviate the fields; delete this method
   */
  private static Article kludgeArticleFields(Article article) {
    ArticleAsset xmlAsset = new ArticleAsset();
    xmlAsset.setDoi(article.getDoi());
    xmlAsset.setExtension("XML");

    article.setID(-1L);
    article.setState(Article.STATE_ACTIVE);
    article.setAssets(ImmutableList.of(xmlAsset));
    article.setJournals(ImmutableSet.<Journal>of());
    article.setRelatedArticles(ImmutableList.<ArticleRelationship>of());
    article.setCategories(ImmutableMap.<Category, Integer>of());
    return article;
  }


  static final String ARCHIVE_ENTRY_NAME_KEY = "archiveEntryName";
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

  @Override
  public IngestionResult writeArchive(Archive inputArchive) throws IOException {
    IngestionResult ingestionResult;
    try {
      ingestionResult = new VersionedIngestionService(this).ingest(inputArchive);
    } catch (XmlContentException e) {
      throw new RestClientException("Invalid XML", HttpStatus.BAD_REQUEST, e);
    } finally {
      inputArchive.close();
    }
    return ingestionResult;
  }

  @Override
  public Article writeToLegacy(ArticleIdentity articleIdentity) throws IOException {
    return new LegacyIngestionService(this).writeArchive(readArchive(articleIdentity));
  }

  @Override
  public Article writeToLegacy(RepoCollectionMetadata articleCollection) throws IOException {
    RepoVersionNumber versionNumber = articleCollection.getVersionNumber();
    ArticleIdentity articleIdentity = new ArticleIdentity(versionNumber.getKey(), Optional.of(versionNumber.getNumber()));
    return writeToLegacy(articleIdentity);
  }

  @Override
  public Archive readArchive(ArticleIdentity articleIdentity) {
    RepoCollectionMetadata collection = fetchArticleCollection(articleIdentity);
    String archiveName = articleIdentity.getLastToken() + ".zip";
    return Archive.readCollection(contentRepoService, archiveName, collection, ARCHIVE_ENTRY_NAME_EXTRACTOR);
  }

  @Override
  public void repopulateCategories(ArticleIdentity id) throws IOException {
    new LegacyIngestionService(this).repopulateCategories(id);
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

  @Override
  public Transceiver readMetadata(final ArticleIdentity id) throws IOException {
    return new EntityTransceiver<Article>() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }

      @Override
      protected Article fetchEntity() {
        Article article = findArticleById(id);
        if (article == null) {
          throw reportNotFound(id);
        }
        return article;
      }

      @Override
      protected Object getView(Article entity) {
        return createArticleView(entity, false);
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
        return null;
      }

      @Override
      protected Object getData() throws IOException {
        Document doc = fetchArticleXml(id);
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
        List<String> rawTerms = taxonomyService.getRawTerms(parseXml(readXml(id)));
        List<String> cleanedTerms = new ArrayList<>();
        for (String term : rawTerms) {
          term = term.replaceAll("<TERM>", "").replaceAll("</TERM>", "");
          cleanedTerms.add(term);
        }
        return cleanedTerms;
      }
    };
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

  @Override
  @Required
  public void setAssetService(AssetCrudService assetService) {
    this.assetService = assetService;
  }
}
