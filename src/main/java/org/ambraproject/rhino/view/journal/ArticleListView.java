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

package org.ambraproject.rhino.view.journal;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.model.ArticleList;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleRevisionView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

public class ArticleListView implements JsonOutputView {

  public static class Factory {
    @Autowired
    private ArticleRevisionView.Factory articleRevisionViewFactory;

    public ArticleListView getView(ArticleList articleList, String journalKey) {
      return new ArticleListView(journalKey, articleList, articleRevisionViewFactory);
    }
  }

  private final String journalKey;
  private final ArticleList articleList;
  private final ArticleRevisionView.Factory articleFactory;

  private ArticleListView(String journalKey, ArticleList articleList,
                          ArticleRevisionView.Factory articleFactory) {
    this.journalKey = Objects.requireNonNull(journalKey);
    this.articleList = Objects.requireNonNull(articleList);
    this.articleFactory = Objects.requireNonNull(articleFactory);
  }

  public ArticleListIdentity getIdentity() {
    String listType = articleList.getListType();
    String listKey = articleList.getListKey();
    return new ArticleListIdentity(listType, journalKey, listKey);
  }

  public ArticleList getArticleList() {
    return articleList;
  }


  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(getIdentity()).getAsJsonObject();
    serialized.addProperty("title", articleList.getDisplayName());

    List<ArticleRevisionView> articleViews = Lists.transform(articleList.getArticles(),
        articleFactory::getLatestRevisionView);
    serialized.add("articles", context.serialize(articleViews));
    return serialized;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleListView that = (ArticleListView) o;
    if (!articleList.equals(that.articleList)) return false;
    if (!journalKey.equals(that.journalKey)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = journalKey.hashCode();
    result = 31 * result + articleList.hashCode();
    return result;
  }
}
