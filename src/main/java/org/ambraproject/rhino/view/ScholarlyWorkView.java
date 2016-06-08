package org.ambraproject.rhino.view;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.ArticleItem;

import java.util.Objects;

public class ScholarlyWorkView implements JsonOutputView {

  private final ArticleItem articleItem;

  public ScholarlyWorkView(ArticleItem articleItem) {
    this.articleItem = Objects.requireNonNull(articleItem);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(articleItem).getAsJsonObject();
    serialized.add("revisionNumber", context.serialize(
        articleItem.getRevisionNumber().map(ImmutableList::of).orElse(ImmutableList.of())));
    return serialized;
  }
}
