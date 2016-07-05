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
 * types for ArticleRelationship
 *
 * @author John Fesenko 6/28/2016
 */
public enum ArticleRelationshipType {
  CORRECTION("correction"),
  RETRACTION("retraction"),
  EOC("expression-of-concern");

  private String string;

  private ArticleRelationshipType(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return this.string;
  }

  public static ArticleRelationshipType fromString(String string) {
    if (CORRECTION.string.equals(string)) {
      return CORRECTION;
    } else if (RETRACTION.string.equals(string)) {
      return RETRACTION;
    } else if (EOC.string.equals(string)) {
      return EOC;
    } else {
      return null;
    }
  }

  public static PersistenceAdapter<ArticleRelationshipType, String> ADAPTER = new PersistenceAdapter<ArticleRelationshipType, String>() {
    @Override
    public Class<ArticleRelationshipType> getModelClass() {
      return ArticleRelationshipType.class;
    }

    @Override
    public Class<String> getDataClass() {
      return String.class;
    }

    @Override
    public String encode(ArticleRelationshipType model) {
      return model.toString();
    }

    @Override
    public ArticleRelationshipType decode(String data) {
      return fromString(data);
    }
  };

}
