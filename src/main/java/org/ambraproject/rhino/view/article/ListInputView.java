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

package org.ambraproject.rhino.view.article;

import com.google.common.base.Optional;
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
import java.util.Set;

public class ListInputView {

  private final Optional<ArticleListIdentity> identity;
  private final Optional<String> title;
  private final Optional<ImmutableSet<ArticleIdentifier>> articleIds;

  private ListInputView(ArticleListIdentity identity, String title, Set<ArticleIdentifier> articleIds) {
    this.identity = Optional.fromNullable(identity);
    this.title = Optional.fromNullable(title);
    this.articleIds = (articleIds == null) ? Optional.<ImmutableSet<ArticleIdentifier>>absent()
        : Optional.of(ImmutableSet.copyOf(articleIds));
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
