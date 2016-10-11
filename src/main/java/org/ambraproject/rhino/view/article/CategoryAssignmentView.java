package org.ambraproject.rhino.view.article;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.ArticleCategoryAssignment;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Objects;

public class CategoryAssignmentView implements JsonOutputView {

  private final ArticleCategoryAssignment categoryAssignment;

  public CategoryAssignmentView(ArticleCategoryAssignment categoryAssignment) {
    this.categoryAssignment = Objects.requireNonNull(categoryAssignment);
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("path", categoryAssignment.getCategory().getPath());
    serialized.addProperty("weight", categoryAssignment.getWeight());
    return serialized;
  }

}
