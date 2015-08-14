package org.ambraproject.rhino.view.article;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.ambraproject.rhino.identity.ArticleIdentity;

import java.lang.reflect.Type;
import java.util.Set;

public class CollectionInputView {

  private final Optional<String> title;
  private final Optional<ImmutableSet<ArticleIdentity>> articleIds;

  private CollectionInputView(String title, Set<ArticleIdentity> articleIds) {
    this.title = Optional.fromNullable(title);
    this.articleIds = (articleIds == null) ? Optional.<ImmutableSet<ArticleIdentity>>absent()
        : Optional.of(ImmutableSet.copyOf(articleIds));
  }

  public Optional<String> getTitle() {
    return title;
  }

  public Optional<ImmutableSet<ArticleIdentity>> getArticleIds() {
    return articleIds;
  }

  public static final JsonDeserializer<CollectionInputView> DESERIALIZER = new JsonDeserializer<CollectionInputView>() {
    @Override
    public CollectionInputView deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      JsonObject jsonObject = json.getAsJsonObject();

      JsonElement titleElement = jsonObject.get("title");
      String title = (titleElement == null || titleElement.isJsonNull()) ? null : titleElement.getAsString();

      JsonElement articleDoisElement = jsonObject.get("articleDois");
      Set<ArticleIdentity> articleIds;
      if (articleDoisElement == null || articleDoisElement.isJsonNull()) {
        articleIds = null;
      } else {
        JsonArray articleDoisArray = articleDoisElement.getAsJsonArray();
        articleIds = Sets.newLinkedHashSetWithExpectedSize(articleDoisArray.size());
        for (JsonElement articleDoiElement : articleDoisArray) {
          String articleDoi = articleDoiElement.getAsString();
          articleIds.add(ArticleIdentity.create(articleDoi));
        }
      }

      return new CollectionInputView(title, articleIds);
    }
  };


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CollectionInputView that = (CollectionInputView) o;

    if (!articleIds.equals(that.articleIds)) return false;
    if (!title.equals(that.title)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = title.hashCode();
    result = 31 * result + articleIds.hashCode();
    return result;
  }
}
