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

package org.ambraproject.rhino.view;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * Class intended for JSON serialization that wraps some other object.  The constructor allows you to specify which
 * properties on the underlying object you want serialized.
 */
public class JsonWrapper<T> implements JsonOutputView {

  private final T target;

  private final String[] propertiesToInclude;

  /**
   * Constructor.
   *
   * @param target              the underlying object to base the serialization on
   * @param propertiesToInclude only these properties of target will be serialized; all others will not be visible
   */
  public JsonWrapper(T target, String... propertiesToInclude) {
    this.target = Preconditions.checkNotNull(target);
    this.propertiesToInclude = propertiesToInclude;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    for (String property : propertiesToInclude) {
      String value;
      try {
        value = (String) PropertyUtils.getSimpleProperty(target, property);
      } catch (IllegalAccessException iae) {
        throw new RuntimeException(iae);
      } catch (InvocationTargetException ite) {
        throw new RuntimeException(ite);
      } catch (NoSuchMethodException nsme) {
        throw new RuntimeException(nsme);
      }
      serialized.addProperty(property, value);
    }
    return serialized;
  }
}
