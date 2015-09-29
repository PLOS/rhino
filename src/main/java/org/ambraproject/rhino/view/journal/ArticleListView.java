package org.ambraproject.rhino.view.journal;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleList;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.view.JsonOutputView;

public class ArticleListView implements JsonOutputView {

  private final String journalKey;
  private final ArticleList articleList;

  public ArticleListView(String journalKey, ArticleList articleList) {
    this.journalKey = Preconditions.checkNotNull(journalKey);
    this.articleList = Preconditions.checkNotNull(articleList);
  }

  public ArticleListIdentity getIdentity() {
    return new ArticleListIdentity(Optional.fromNullable(articleList.getListType()),
        journalKey, articleList.getListCode());
  }

  public ArticleList getArticleList() {
    return articleList;
  }


  private JsonObject serializeArticle(JsonSerializationContext context, Article article) {
    JsonObject articleIdObj = new JsonObject();
    articleIdObj.addProperty("doi", article.getDoi());
    articleIdObj.addProperty("title", article.getTitle());
    return articleIdObj;
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(getIdentity()).getAsJsonObject();
    serialized.addProperty("title", articleList.getDisplayName());

    JsonArray articleIdList = new JsonArray();
    for (Article article : articleList.getArticles()) {
      articleIdList.add(serializeArticle(context, article));
    }
    serialized.add("articles", articleIdList);

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
