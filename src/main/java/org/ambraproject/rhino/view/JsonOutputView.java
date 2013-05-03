package org.ambraproject.rhino.view;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public interface JsonOutputView {

  public abstract JsonElement serialize(JsonSerializationContext context);

  public static final JsonSerializer<JsonOutputView> SERIALIZER = new JsonSerializer<JsonOutputView>() {
    @Override
    public JsonElement serialize(JsonOutputView src, Type typeOfSrc, JsonSerializationContext context) {
      return src.serialize(context);
    }
  };

}
