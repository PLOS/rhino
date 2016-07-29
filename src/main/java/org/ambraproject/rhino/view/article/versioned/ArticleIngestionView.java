package org.ambraproject.rhino.view.article.versioned;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.impl.VersionedIngestionService;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.rhino.view.JsonOutputView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public class ArticleIngestionView implements JsonOutputView {

  public static class Factory {

    @Autowired
    private ArticleCrudService articleCrudService;
    @Autowired
    private VersionedIngestionService versionedIngestionService;
    @Autowired
    private RelationshipSetView.Factory relationshipSetViewFactory;

    public ArticleIngestionView getView(ArticleIngestionIdentifier ingestionId) {
      ArticleIngestion ingestion = articleCrudService.readIngestion(ingestionId);
      ArticleMetadata metadata = versionedIngestionService.getArticleMetadata(ingestionId);
      RelationshipSetView relationships = relationshipSetViewFactory.getView(metadata);

      return new ArticleIngestionView(ingestion, metadata, relationships);
    }

  }

  private final ArticleIngestion ingestion;
  private final ArticleMetadata metadata;
  private final RelationshipSetView relationships;

  private ArticleIngestionView(ArticleIngestion ingestion,
                               ArticleMetadata metadata,
                               RelationshipSetView relationships) {
    Preconditions.checkArgument(ingestion.getArticle().getDoi().equals(metadata.getDoi()));
    this.ingestion = ingestion;
    this.metadata = metadata;

    this.relationships = Objects.requireNonNull(relationships);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", ingestion.getArticle().getDoi());
    serialized.addProperty("ingestionNumber", ingestion.getIngestionNumber());

    JsonAdapterUtil.copyWithoutOverwriting(context.serialize(metadata).getAsJsonObject(), serialized);
    serialized.remove("assets"); // TODO: Create groomed view for ArticleItem/ArticleFile objects
    serialized.remove("authors");
    serialized.remove("collaborativeAuthors");

    serialized.add("relatedArticles", context.serialize(relationships));

    return serialized;
  }

}
