package org.ambraproject.rhino.content.view;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.ambraproject.rhino.identity.ArticleIdentity;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;

/**
 * A list of article views that are keyed by DOI. The list should be serialized as a JSON object: the members are the
 * objects in the list, and the member names are the objects' REST IDs. An object's REST ID is the same as its DOI
 * without the {@code "info:doi/"} prefix.
 */
public class ArticleViewList {

  private final Collection<? extends ArticleView> views;

  public ArticleViewList(Collection<? extends ArticleView> views) {
    this.views = Collections.unmodifiableCollection(views);
  }

  public static final JsonSerializer<ArticleViewList> SERIALIZER = new JsonSerializer<ArticleViewList>() {
    @Override
    public JsonElement serialize(ArticleViewList src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject serializedList = new JsonObject();
      for (ArticleView view : src.views) {
        String key = ArticleIdentity.removeScheme(view.getDoi());
        serializedList.add(key, context.serialize(view));
      }
      return serializedList;
    }
  };

}
