/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2014 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.view.article;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.views.AuthorView;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON output view class for authors of an article.
 * <p/>
 * This class would ideally be named just AuthorView, but the code also uses org.ambraproject.views.AuthorView, and I
 * thought it best to choose a distinct name.
 */
public class ArticleAuthorView implements JsonOutputView {

  private AuthorView authorView;

  private ArticleAuthorView(AuthorView authorView) {
    this.authorView = authorView;
  }

  /**
   * Creates a list of views given a list of authors.
   *
   * @param authors list of AuthorViews to convert
   * @return list of ArticleAuthorViews
   */
  public static List<ArticleAuthorView> createList(List<AuthorView> authors) {
    List<ArticleAuthorView> results = new ArrayList<>(authors.size());
    for (AuthorView author : authors) {
      results.add(new ArticleAuthorView(author));
    }
    return results;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("fullName", authorView.getFullName());
    JsonObject baseJson = context.serialize(authorView).getAsJsonObject();
    serialized = JsonAdapterUtil.copyWithoutOverwriting(baseJson, serialized);
    return serialized;
  }
}
