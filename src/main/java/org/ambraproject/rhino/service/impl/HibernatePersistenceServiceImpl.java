package org.ambraproject.rhino.service.impl;

import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.identity.ArticleIdentifier;
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
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.JournalCrudService;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class HibernatePersistenceServiceImpl implements HibernatePersistenceService {

  @Autowired
  private HibernateTemplate hibernateTemplate;
  @Autowired
  private JournalCrudService journalCrudService;
  @Autowired
  private ContentRepoPersistenceService contentRepoPersistenceService;

  private static final int FIRST_INGESTION_NUMBER = 1;

  @Override
  public Article persistArticle(ArticleIdentifier articleIdentifier) {
    String articleDoi = articleIdentifier.getDoiName();
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
  public ArticleIngestion persistIngestion(Article article, ArticleMetadata articleMetadata,
                                           ArticleCustomMetadata customMetadata) {
    Journal journal = fetchJournal(articleMetadata);

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
    ingestion.setTitle(articleMetadata.getTitle());
    ingestion.setPublicationDate(java.sql.Date.valueOf(articleMetadata.getPublicationDate()));
    ingestion.setArticleType(articleMetadata.getArticleType());
    ingestion.setJournal(journal);

    ingestion.setRevisionDate((customMetadata.getRevisionDate() == null ? null
        : java.sql.Date.valueOf(customMetadata.getRevisionDate())));
    ingestion.setPublicationStage(customMetadata.getPublicationStage());

    hibernateTemplate.save(ingestion);
    return ingestion;
  }

  @Override
  public void persistAssets(ArticlePackage articlePackage, ArticleIngestion ingestion,
                            ManifestXml manifest) {
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

    persistStrikingImage(ingestion, items, manifest);
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

  private Journal fetchJournal(ArticleMetadata article) {
    String eissn = article.geteIssn();
    if (eissn == null) {
      String msg = "eIssn not set for article: " + article.getDoi();
      throw new RestClientException(msg, HttpStatus.BAD_REQUEST);
    }
    return journalCrudService.getJournalByEissn(eissn).orElseThrow(() -> {
      String msg = "XML contained eIssn that was not matched to a journal: " + eissn;
      return new RestClientException(msg, HttpStatus.BAD_REQUEST);
    });
  }
}
