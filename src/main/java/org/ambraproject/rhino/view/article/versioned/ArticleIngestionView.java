package org.ambraproject.rhino.view.article.versioned;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.VersionedArticleRelationship;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.rhino.view.JsonOutputView;

public class ArticleIngestionView implements JsonOutputView {

  private final ArticleIngestion ingestion;
  private final ArticleMetadata metadata;

  ArticleIngestionView(ArticleIngestion ingestion, ArticleMetadata metadata) {
    Preconditions.checkArgument(ingestion.getArticle().getDoi().equals(metadata.getDoi()));
    this.ingestion = ingestion;
    this.metadata = metadata;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", ingestion.getArticle().getDoi());
    serialized.addProperty("ingestionNumber", ingestion.getIngestionNumber());

    JsonAdapterUtil.copyWithoutOverwriting(context.serialize(metadata).getAsJsonObject(), serialized);

    return serialized;
  }

}
