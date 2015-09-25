package org.ambraproject.rhino.view.article;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleList;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.List;

public class DeepArticleListView implements JsonOutputView {

  private final ArticleList articleList;

  public DeepArticleListView(ArticleList articleList) {
    this.articleList = Preconditions.checkNotNull(articleList);
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject serializedList = context.serialize(articleList).getAsJsonObject();

    JsonArray serializedArticles = new JsonArray();
    for (Article article : articleList.getArticles()) {
      serializedArticles.add(serialize(article));
    }
    serializedList.add("articles", serializedArticles);

    return serializedList;
  }

  private JsonObject serialize(Article article) {
    JsonObject serializedArticle = new JsonObject();

    // These are motivated by the historic use case of the "In the News" front end view. TODO: Include more?
    serializedArticle.addProperty("doi", article.getDoi());
    serializedArticle.addProperty("title", article.getTitle());

    return serializedArticle;
  }

}
