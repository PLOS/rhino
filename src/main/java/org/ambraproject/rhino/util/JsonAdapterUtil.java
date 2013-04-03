package org.ambraproject.rhino.util;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

public class JsonAdapterUtil {
  private JsonAdapterUtil() {
    throw new AssertionError("Not instantiable");
  }

  /**
   * Copy all members that aren't already present.
   *
   * @param source      the object to copy from
   * @param destination the object to copy members to
   * @return {@code destination}
   */
  public static JsonObject copyWithoutOverwriting(JsonObject source, JsonObject destination) {
    Preconditions.checkNotNull(destination);
    for (Map.Entry<String, JsonElement> fromEntry : source.entrySet()) {
      String key = fromEntry.getKey();
      if (!destination.has(key)) {
        destination.add(key, fromEntry.getValue());
      }
    }
    return destination;
  }

}
