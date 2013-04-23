package org.ambraproject.rhino.content.view;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.ambraproject.rhino.identity.ArticleIdentity;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;

public class DoiList {

  private final Collection<String> dois;

  public DoiList(Collection<String> dois) {
    this.dois = Collections.unmodifiableCollection(dois);
  }

  public static final JsonSerializer<DoiList> SERIALIZER = new JsonSerializer<DoiList>() {
    @Override
    public JsonElement serialize(DoiList src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject serializedList = new JsonObject();
      for (String doi : src.dois) {
        String key = ArticleIdentity.removeScheme(doi);
        JsonObject serializedItem = new JsonObject();
        serializedItem.addProperty(ArticleJsonConstants.MemberNames.DOI, doi);
        serializedList.add(key, serializedItem);
      }
      return serializedList;
    }
  };

}
