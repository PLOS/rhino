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

package org.ambraproject.rhino.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Arrays;

public interface PersistenceAdapter<M, D> {

  public abstract Class<M> getModelClass();

  public abstract Class<D> getDataClass();

  public abstract D encode(M model);

  public abstract M decode(D data);

  public static <E extends Enum<E>> PersistenceAdapter<E, String> byEnumName(Class<E> enumType){
    ImmutableMap<String, E> enumsByName = Maps.uniqueIndex(Arrays.asList(enumType.getEnumConstants()), E::name);
    return new PersistenceAdapter<E, String>() {
      @Override
      public Class<E> getModelClass() {
        return enumType;
      }

      @Override
      public Class<String> getDataClass() {
        return String.class;
      }

      @Override
      public String encode(E model) {
        return model.name();
      }

      @Override
      public E decode(String data) {
        return enumsByName.get(data);
      }
    };
  }

}
