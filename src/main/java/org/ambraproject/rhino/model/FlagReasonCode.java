/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2012 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
