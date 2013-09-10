package org.ambraproject.rhino.util;

import com.google.common.base.Preconditions;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.ambraproject.rhombat.gson.Iso8601CalendarAdapter;
import org.ambraproject.rhombat.gson.Iso8601DateAdapter;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

public final class JsonAdapterUtil {
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

  /**
   * Create a {@code GsonBuilder} preconfigured with utility adapters.
   *
   * @return a {@code GsonBuilder} object
   */
  public static GsonBuilder makeGsonBuilder() {
    GsonBuilder builder = new GsonBuilder();

    builder.registerTypeAdapter(Date.class, new Iso8601DateAdapter());
    Iso8601CalendarAdapter calendarAdapter = new Iso8601CalendarAdapter();
    builder.registerTypeAdapter(Calendar.class, calendarAdapter);
    builder.registerTypeAdapter(GregorianCalendar.class, calendarAdapter);

    return builder;
  }

}
