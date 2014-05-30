package org.ambraproject.rhino.config.json;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;

import java.lang.reflect.Type;

public class DoiBasedIdentitySerializer implements JsonSerializer<DoiBasedIdentity> {
  private DoiBasedIdentitySerializer() {
  }

  public static final DoiBasedIdentitySerializer INSTANCE = new DoiBasedIdentitySerializer();

  public void register(GsonBuilder builder) {
    builder.registerTypeAdapter(DoiBasedIdentity.class, this);
    builder.registerTypeAdapter(ArticleIdentity.class, this);
  }

  @Override
  public JsonElement serialize(DoiBasedIdentity src, Type typeOfSrc, JsonSerializationContext context) {
    return context.serialize(src.getIdentifier());
  }
}
