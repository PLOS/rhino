package org.ambraproject.rhino.view.article;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.model.ArticleCollection;
import org.ambraproject.rhino.view.JsonOutputView;

public class CollectionView implements JsonOutputView {

  private final ArticleCollection articleCollection;

  CollectionView(ArticleCollection articleCollection) {
    this.articleCollection = articleCollection;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();

    serialized.add("journalKey", context.serialize(articleCollection.getJournal().getJournalKey()));
    serialized.add("slug", context.serialize(articleCollection.getSlug()));
    serialized.add("title", context.serialize(articleCollection.getTitle()));

    JsonArray articleIds = new JsonArray();
    for (Article article : articleCollection.getArticles()) {
      String articleIdString = ArticleIdentity.create(article).getIdentifier();
      articleIds.add(context.serialize(articleIdString));
    }
    serialized.add("articles", articleIds);

    return serialized;
  }

}
