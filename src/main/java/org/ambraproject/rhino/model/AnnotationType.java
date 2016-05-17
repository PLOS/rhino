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
 * Types of annotations
 */
public enum AnnotationType {
  COMMENT("Comment"), //comment on an article
  REPLY("Reply");

  private String string;

  private AnnotationType(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return this.string;
  }

  public static AnnotationType fromString(String string) {
    if (COMMENT.string.equals(string)) {
      return COMMENT;
    } else if (REPLY.string.equals(string)) {
      return REPLY;
      // TODO: Remove after deleting Formal Corrections/Retractions from db
    } else if (string.equalsIgnoreCase("FormalCorrection") || string.equalsIgnoreCase("Retraction")) {
      return null;
    } else {
      throw new IllegalArgumentException("Unknown annotation type: " + string);
    }
  }

  public static final PersistenceAdapter<AnnotationType, String> ADAPTER = new PersistenceAdapter<AnnotationType, String>() {
    @Override
    public Class<AnnotationType> getModelClass() {
      return AnnotationType.class;
    }

    @Override
    public Class<String> getDataClass() {
      return String.class;
    }

    @Override
    public String encode(AnnotationType model) {
      return model.toString();
    }

    @Override
    public AnnotationType decode(String data) {
      try {
        return fromString(data);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
  };

}