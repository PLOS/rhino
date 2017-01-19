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

package org.ambraproject.rhino.service.taxonomy;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.util.Objects;

public final class WeightedTerm {
  private final String path;
  private final int weight;

  public WeightedTerm(String path, int weight) {
    this.path = Objects.requireNonNull(path);
    this.weight = weight;
  }

  public String getPath() {
    return path;
  }

  public int getWeight() {
    return weight;
  }


  private static final Splitter TERM_SPLITTER = Splitter.on('/');

  public String getLeafTerm() {
    return Iterables.getLast(TERM_SPLITTER.split(path));
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WeightedTerm that = (WeightedTerm) o;

    if (weight != that.weight) return false;
    if (!path.equals(that.path)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = path.hashCode();
    result = 31 * result + weight;
    return result;
  }

  @Override
  public String toString() {
    return "WeightedTerm{" +
        "path='" + path + '\'' +
        ", weight=" + weight +
        '}';
  }
}
