package org.ambraproject.rhino.view.article;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Objects;

/**
 * An object describing an article's visibility: whether it is in a published state, and the set of journals in which it
 * has been published.
 */
public class ArticleVisibility implements JsonOutputView {

  private final Doi doi;

  private ArticleVisibility(Doi doi) {
    this.doi = Objects.requireNonNull(doi);
  }

  public static ArticleVisibility create(Doi doi) {
    return new ArticleVisibility(doi);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.add("doi", context.serialize(doi.getName()));
    return serialized;
  }
}
