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

import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.article.ArticleCustomMetadata;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.ingest.ArticleItemInput;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.model.ingest.IngestPackage;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.ContentPersistenceService;
import org.ambraproject.rhino.service.HibernatePersistenceService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class HibernatePersistenceServiceImpl implements HibernatePersistenceService {

  @Autowired
  private HibernateTemplate hibernateTemplate;
  @Autowired
  private JournalCrudService journalCrudService;
  @Autowired
  private ContentPersistenceService contentRepoPersistenceService;
  @Autowired
  private ConfigurationReadService configurationReadService;

  private static final int FIRST_INGESTION_NUMBER = 1;

  @Override
  public Article persistArticle(Doi doi) {
    String articleDoi = doi.getName();
    Article article = hibernateTemplate.execute(session -> {
      Query selectQuery = session.createQuery("FROM Article WHERE doi = :doi");
      selectQuery.setParameter("doi", articleDoi);
      return (Article) selectQuery.uniqueResult();
    });
    if (article == null) {
      article = new Article();
      article.setDoi(articleDoi);
      hibernateTemplate.save(article);
    }
    return article;
  }

  @Override
  public ArticleIngestion persistIngestion(Article article, IngestPackage ingestPackage) {
    Journal journal = fetchJournal(ingestPackage);

    int nextIngestionNumber = hibernateTemplate.execute(session -> {
      Query findNextIngestionNumber = session.createQuery(
          "SELECT MAX(ingestionNumber) FROM ArticleIngestion WHERE article = :article");
      findNextIngestionNumber.setParameter("article", article);
      Number maxIngestionNumber = (Number) findNextIngestionNumber.uniqueResult();
      return (maxIngestionNumber == null) ? FIRST_INGESTION_NUMBER
          : maxIngestionNumber.intValue() + 1;
    });

    ArticleIngestion ingestion = new ArticleIngestion();
    ingestion.setArticle(article);
    ingestion.setIngestionNumber(nextIngestionNumber);

    ArticleMetadata articleMetadata = ingestPackage.getArticleMetadata();
    ingestion.setTitle(articleMetadata.getTitle());
    ingestion.setPublicationDate(java.sql.Date.valueOf(articleMetadata.getPublicationDate()));
    ingestion.setArticleType(articleMetadata.getArticleType());
    ingestion.setJournal(journal);

    ArticleCustomMetadata customMetadata = ingestPackage.getArticleCustomMetadata();
    ingestion.setRevisionDate((customMetadata.getRevisionDate() == null ? null
        : java.sql.Date.valueOf(customMetadata.getRevisionDate())));
    ingestion.setPublicationStage(customMetadata.getPublicationStage());

    hibernateTemplate.save(ingestion);
    return ingestion;
  }

  @Override
  public void persistAssets(ArticlePackage articlePackage, ArticleIngestion ingestion) {
    List<ArticleItem> items = articlePackage.getAllItems().stream()
        .map((ArticleItemInput item) -> contentRepoPersistenceService.createItem(item, ingestion))
        .collect(Collectors.toList());
    for (ArticleItem item : items) {
      hibernateTemplate.save(item);
    }

    Collection<ArticleFile> files = contentRepoPersistenceService.persistAncillaryFiles(articlePackage,
        ingestion);
    for (ArticleFile file : files) {
      hibernateTemplate.save(file);
    }

    persistStrikingImage(ingestion, items, articlePackage.getManifest());
  }

  @Override
  public Optional<ArticleItem> persistStrikingImage(ArticleIngestion ingestion,
                                                    List<ArticleItem> items, ManifestXml manifest) {
    Optional<ManifestXml.Asset> strikingImageAsset = manifest.getAssets().stream()
        .filter(ManifestXml.Asset::isStrikingImage)
        .findAny();
    if (!strikingImageAsset.isPresent()) {
      return Optional.empty(); // No striking image declared, so go without setting one.
    }

    Doi strikingImageDoi = Doi.create(strikingImageAsset.get().getUri());
    Optional<ArticleItem> strikingImageItem = items.stream()
        .filter(item -> Doi.create(item.getDoi()).equals(strikingImageDoi))
        .findAny();
    if (!strikingImageItem.isPresent()) {
      throw new RuntimeException("Striking image from manifest not found (should have been created by now)");
    }

    ingestion.setStrikingImage(strikingImageItem.get());
    hibernateTemplate.update(ingestion);
    return strikingImageItem;
  }

  private Journal fetchJournal(IngestPackage ingestPackage) {
    final ArticleMetadata articleMetadata = ingestPackage.getArticleMetadata();
    final String eissn = articleMetadata.geteIssn();

    Optional<Journal> journal;
    if (eissn != null) {
      journal = getJournalFromEissn(eissn);
    } else {
      throw new RestClientException("eIssn not set for article: " + articleMetadata.getDoi(),
          HttpStatus.BAD_REQUEST);
    }
    return journal.orElse(null);
  }

  private Optional<Journal> getJournalFromEissn(String eissn) {
    Optional<Journal> journal;
    journal = journalCrudService.getJournalByEissn(eissn);
    if (!journal.isPresent()) {
      String msg = "XML contained eIssn that was not matched to a journal: " + eissn;
      throw new RestClientException(msg, HttpStatus.BAD_REQUEST);
    }
    return journal;
  }
}
