package org.ambraproject.rhino.view.article;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Category;
import org.ambraproject.rhino.view.JsonOutputView;

public class CategoryView implements JsonOutputView {

  private final Category category;
  private final int weight;

  public CategoryView(Category category, int weight) {
    this.category = Preconditions.checkNotNull(category);
    this.weight = weight;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(category).getAsJsonObject();
    serialized.addProperty("weight", weight);
    return serialized;
  }
}
