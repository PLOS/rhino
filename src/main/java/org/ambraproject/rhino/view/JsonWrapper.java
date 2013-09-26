/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.view;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * Class intended for JSON serialization that wraps some other object.  The constructor
 * allows you to specify which properties on the underlying object you want serialized.
 */
public class JsonWrapper<T> implements JsonOutputView {

  private final T target;

  private final String[] propertiesToInclude;

  /**
   * Constructor.
   *
   * @param target the underlying object to base the serialization on
   * @param propertiesToInclude only these properties of target will be serialized; all others
   *     will not be visible
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
