package org.ambraproject.rhino.view.journal;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleList;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleOutputView;

import java.util.List;

public class ArticleListView implements JsonOutputView {

  private final String journalKey;
  private final ArticleList articleList;

  public ArticleListView(String journalKey, ArticleList articleList) {
    this.journalKey = Preconditions.checkNotNull(journalKey);
    this.articleList = Preconditions.checkNotNull(articleList);
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

    List<ArticleOutputView> articleIdList = Lists.transform(articleList.getArticles(), ArticleOutputView::createMinimalView);
    serialized.add("articles", context.serialize(articleIdList));

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
