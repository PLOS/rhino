package org.ambraproject.rhino.view.article.versioned;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.view.JsonOutputView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.Optional;

/**
 * A representation of an {@link ArticleTable} object. If it has one or more revisions, include metadata for a revision,
 * but <em>only</em> metadata that can be gotten from the database, without parsing the manuscript.
 */
public class PersistentArticleView implements JsonOutputView {

  public static class Factory {
    @Autowired
    private ArticleCrudService articleCrudService;

    /**
     * @param article the article to represent
     * @return a view representing the article and, if it has one, its latest revision
     */
    public PersistentArticleView getView(ArticleTable article) {
      return new PersistentArticleView(article, articleCrudService.getLatestRevision(article));
    }
  }

  /**
   * @param revision the revision to represent
   * @return a view representing the parent article and the revision
   */
  public PersistentArticleView getView(ArticleRevision revision) {
    return new PersistentArticleView(revision.getIngestion().getArticle(), Optional.of(revision));
  }


  private final ArticleTable article;
  private final Optional<ArticleRevision> latestRevision;

  private PersistentArticleView(ArticleTable article, Optional<ArticleRevision> latestRevision) {
    this.article = Objects.requireNonNull(article);
    this.latestRevision = Objects.requireNonNull(latestRevision);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", article.getDoi());

    this.latestRevision.ifPresent(latestRevision -> {
      serialized.addProperty("revisionNumber", latestRevision.getRevisionNumber());

      ArticleIngestion ingestion = latestRevision.getIngestion();
      serialized.addProperty("ingestionNumber", ingestion.getIngestionNumber());
      serialized.addProperty("title", ingestion.getTitle());
      serialized.addProperty("publicationDate", ingestion.getPublicationDate().toLocalDate().toString());
      serialized.addProperty("articleType", ingestion.getArticleType());
      // TODO: Journal of publication (when there is only one)?
      // TODO: Revision date?
    });

    return serialized;
  }

}
