package org.ambraproject.rhino.view;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.identity.ArticleIdentity;

import java.util.Collections;
import java.util.List;

/**
 * A list of article views that are keyed by DOI. The list should be serialized as a JSON object: the members are the
 * objects in the list, and the member names are the objects' REST IDs. An object's REST ID is the same as its DOI
 * without the {@code "info:doi/"} prefix.
 */
public class ArticleViewList implements JsonOutputView {

  private final List<? extends ArticleView> views;

  public ArticleViewList(List<? extends ArticleView> views) {
    // Prefer an O(1) wrapper to ImmutableList because it generally will be a big list
    this.views = Collections.unmodifiableList(views);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serializedList = new JsonObject();
    for (ArticleView view : views) {
      String key = ArticleIdentity.removeScheme(view.getDoi());
      serializedList.add(key, context.serialize(view));
    }
    return serializedList;
  }

}
