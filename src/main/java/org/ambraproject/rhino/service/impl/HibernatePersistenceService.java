package org.ambraproject.rhino.service.impl;

import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.article.ArticleCustomMetadata;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.ingest.ArticlePackage;

import java.util.List;
import java.util.Optional;

public interface HibernatePersistenceService {

  /**
   * Get the article object for a DOI if it exists, and save it if it doesn't.
   *
   * @param articleIdentifier
   */
  public Article persistArticle(ArticleIdentifier articleIdentifier);

  public ArticleIngestion persistIngestion(Article article, ArticleMetadata articleMetadata,
                                           ArticleCustomMetadata customMetadata);

  /**
   * Persist items, items' file representations, ancillary files, and the link to the striking image.
   */
  public void persistAssets(ArticlePackage articlePackage, ArticleIngestion ingestion,
                            ManifestXml manifest);

  public Optional<ArticleItem> persistStrikingImage(ArticleIngestion ingestion,
                                                    List<ArticleItem> items, ManifestXml manifest);
}
