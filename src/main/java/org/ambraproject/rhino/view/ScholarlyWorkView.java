package org.ambraproject.rhino.view;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.ScholarlyWork;

import java.util.Objects;

public class ScholarlyWorkView implements JsonOutputView {

  private final ScholarlyWork scholarlyWork;

  public ScholarlyWorkView(ScholarlyWork scholarlyWork) {
    this.scholarlyWork = Objects.requireNonNull(scholarlyWork);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(scholarlyWork).getAsJsonObject();
    serialized.add("revisionNumber", context.serialize(
        scholarlyWork.getRevisionNumber().map(ImmutableList::of).orElse(ImmutableList.of())));
    return serialized;
  }
}
