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
