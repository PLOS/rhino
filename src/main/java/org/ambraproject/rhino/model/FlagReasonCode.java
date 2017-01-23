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

import org.ambraproject.rhino.config.PersistenceAdapter;

/**
 * Reason code for flags
 *
 * @author Alex Kudlick 3/8/12
 */
public enum FlagReasonCode {
  SPAM("spam"),
  OFFENSIVE("offensive"),
  INAPPROPRIATE("inappropriate"),
  CORRECTION("Create Correction"),
  OTHER("other");

  private String string;

  private FlagReasonCode(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return this.string;
  }

  public static FlagReasonCode fromString(String string) {
    if (SPAM.string.equals(string)) {
      return SPAM;
    } else if (OFFENSIVE.string.equals(string)) {
      return OFFENSIVE;
    } else if (INAPPROPRIATE.string.equals(string)) {
      return INAPPROPRIATE;
    } else if (CORRECTION.string.equals(string)) {
      return CORRECTION;
    } else {
      return OTHER;
    }
  }

  public static PersistenceAdapter<FlagReasonCode, String> ADAPTER = new PersistenceAdapter<FlagReasonCode, String>() {
    @Override
    public Class<FlagReasonCode> getModelClass() {
      return FlagReasonCode.class;
    }

    @Override
    public Class<String> getDataClass() {
      return String.class;
    }

    @Override
    public String encode(FlagReasonCode model) {
      return model.toString();
    }

    @Override
    public FlagReasonCode decode(String data) {
      return fromString(data);
    }
  };

}
