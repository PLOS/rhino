package org.ambraproject.rhino.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;

import java.time.Instant;
import java.time.LocalDate;

public class Java8TimeGsonAdapters {
  private Java8TimeGsonAdapters() {
    throw new AssertionError();
  }

  public static void register(GsonBuilder gsonBuilder) {
    gsonBuilder.registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>)
        (src, type, context) -> context.serialize(src.toString()));
    gsonBuilder.registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>)
        (json, type, context) -> LocalDate.parse(json.getAsString()));
    gsonBuilder.registerTypeAdapter(Instant.class, (JsonSerializer<Instant>)
        (src, type, context) -> context.serialize(src.toString()));
    gsonBuilder.registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>)
        (json, type, context) -> Instant.parse(json.getAsString()));
  }

}
