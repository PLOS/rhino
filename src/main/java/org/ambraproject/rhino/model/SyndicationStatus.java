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

package org.ambraproject.rhino.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.EnumSet;

public enum SyndicationStatus {
  /**
   * This Article has been published, but has not yet been submitted to this syndication target.
   */
  PENDING,
  /**
   * This Article has been submitted to this syndication target, but the process is not yet complete.
   */
  IN_PROGRESS,
  /**
   * This Article has been successfully submitted to this syndication target.
   */
  SUCCESS,
  /**
   * This Article was submitted to this syndication target, but the process failed. The reason for this failure should
   * be written into the <i>errorMessage</i> variable.
   */
  FAILURE;

  private final String label;

  private SyndicationStatus() {
    this.label = name();
  }

  /**
   * @return the string used as a persistent value
   */
  public String getLabel() {
    return label;
  }

  private static final ImmutableMap<String, SyndicationStatus> BY_LABEL = Maps.uniqueIndex(
      EnumSet.allOf(SyndicationStatus.class), SyndicationStatus::getLabel);

  public static ImmutableSet<String> getValidLabels() {
    return BY_LABEL.keySet();
  }

}
