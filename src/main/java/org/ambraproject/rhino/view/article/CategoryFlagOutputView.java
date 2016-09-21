package org.ambraproject.rhino.view.article;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.ArticleCategoryAssignmentFlag;
import org.ambraproject.rhino.view.JsonOutputView;

public class CategoryFlagOutputView implements JsonOutputView {

  private final ArticleCategoryAssignmentFlag flag;

  public CategoryFlagOutputView(ArticleCategoryAssignmentFlag flag) {
    this.flag = flag;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(flag).getAsJsonObject();
    return serialized;
  }
}
