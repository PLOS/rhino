/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.util;

import com.google.common.base.Preconditions;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.ambraproject.rhombat.gson.Iso8601CalendarAdapter;
import org.ambraproject.rhombat.gson.Iso8601DateAdapter;
import org.hibernate.proxy.HibernateProxy;

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
    builder.setDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");

    return builder;
  }

  /**
   * If the object is a lazy-loaded HibernateProxy, force it to be replaced with its actual write-replacement. This can
   * be used to work around Hibernate optimizations that disrupt Gson's automatic serialization.
   * <p/>
   * This may incur extra performance costs if the full object would not have otherwise been read.
   *
   * @param object     a Hibernate model entity, which may be lazy-loaded
   * @param classToken the model type
   * @param <T>        the model type
   * @return a non-lazy-loading instance of the entity
   * @see org.hibernate.proxy.HibernateProxy#writeReplace()
   */
  public static <T> T forceHibernateLazyLoad(T object, Class<T> classToken) {
    Preconditions.checkNotNull(classToken);
    if (object instanceof HibernateProxy) {
      HibernateProxy hibernateProxy = (HibernateProxy) object;
      return classToken.cast(hibernateProxy.writeReplace());
    }
    return object;
  }

}
