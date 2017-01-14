package org.ambraproject.rhino.view.article;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleListIdentity;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class ListInputView {

  private final Optional<ArticleListIdentity> identity;
  private final Optional<String> title;
  private final Optional<ImmutableSet<ArticleIdentifier>> articleIds;

  private ListInputView(ArticleListIdentity identity, String title, Set<ArticleIdentifier> articleIds) {
    this.identity = Optional.ofNullable(identity);
    this.title = Optional.ofNullable(title);
    this.articleIds = Optional.ofNullable(articleIds).map(ImmutableSet::copyOf);
  }

  public Optional<ArticleListIdentity> getIdentity() {
    return identity;
  }

  public Optional<String> getTitle() {
    return title;
  }

  public Optional<ImmutableSet<ArticleIdentifier>> getArticleIds() {
    return articleIds;
  }


  // Helper class that defines the JSON input contract for ListInputView. Deserialized by reflection.
  private static class RawInput {
    private String type;
    private String journal;
    private String key;
    private String title;
    private Collection<String> articleDois;
  }

  public static final JsonDeserializer<ListInputView> DESERIALIZER = new JsonDeserializer<ListInputView>() {
    @Override
    public ListInputView deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
      RawInput inp = context.deserialize(json, RawInput.class);

      final ArticleListIdentity identity;
      if (inp.type != null && inp.journal != null && inp.key != null) {
        identity = new ArticleListIdentity(inp.type, inp.journal, inp.key);
      } else if (inp.type == null && inp.journal == null && inp.key == null) {
        identity = null;
      } else {
        throw new PartialIdentityException();
      }

      final Set<ArticleIdentifier> articleIds;
      if (inp.articleDois == null) {
        articleIds = null;
      } else {
        articleIds = Sets.newLinkedHashSetWithExpectedSize(inp.articleDois.size());
        for (String articleDoi : inp.articleDois) {
          articleIds.add(ArticleIdentifier.create(articleDoi));
        }
      }

      return new ListInputView(identity, inp.title, articleIds);
    }
  };

  /**
   * Indicates that at least one, but not all, of the components of an {@link ArticleListIdentity} were parsed.
   */
  public static class PartialIdentityException extends RuntimeException {
    private PartialIdentityException() {
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ListInputView that = (ListInputView) o;

    if (!articleIds.equals(that.articleIds)) return false;
    if (!identity.equals(that.identity)) return false;
    if (!title.equals(that.title)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = identity.hashCode();
    result = 31 * result + title.hashCode();
    result = 31 * result + articleIds.hashCode();
    return result;
  }
}
