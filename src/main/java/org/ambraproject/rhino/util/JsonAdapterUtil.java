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

import java.util.Date;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.typeadapters.UtcDateTypeAdapter;

import org.hibernate.proxy.HibernateProxy;

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
    builder.registerTypeAdapter(Date.class, new UtcDateTypeAdapter());
    return builder;
  }
}
