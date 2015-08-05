package org.ambraproject.rhino.view.article;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.model.ArticleLink;
import org.ambraproject.rhino.view.JsonOutputView;

public class CollectionView implements JsonOutputView {

  private final ArticleLink articleLink;

  CollectionView(ArticleLink articleLink) {
    this.articleLink = articleLink;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();

    serialized.add("journalKey", context.serialize(articleLink.getJournal().getJournalKey()));
    serialized.add("slug", context.serialize(articleLink.getTarget()));
    serialized.add("title", context.serialize(articleLink.getTitle()));

    JsonArray articleIds = new JsonArray();
    for (Article article : articleLink.getArticles()) {
      String articleIdString = ArticleIdentity.create(article).getIdentifier();
      articleIds.add(context.serialize(articleIdString));
    }
    serialized.add("articles", articleIds);

    return serialized;
  }

}
