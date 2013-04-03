package org.ambraproject.rhino.content;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.ambraproject.models.Article;
import org.ambraproject.models.Syndication;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.service.article.NoSuchArticleIdException;
import org.ambraproject.service.syndication.SyndicationService;

import java.lang.reflect.Type;
import java.util.Collection;

/**
 * A view of an article for printing to JSON. When serialized to a JSON object, it should contain all the same fields as
 * a default dump of the article, plus a few added or augmented.
 */
public class ArticleOutputView {

  /**
   * The state constants given in {@link Article}, with names chosen for this API.
   */
  private static enum PublicationState {
    PUBLISHED(Article.STATE_ACTIVE),
    INGESTED(Article.STATE_UNPUBLISHED),
    DISABLED(Article.STATE_DISABLED);

    private final int constant;

    private PublicationState(int constant) {
      this.constant = constant;
    }

    public static final ImmutableMap<Integer, PublicationState> BY_CONSTANT =
        Maps.uniqueIndex(Iterators.forArray(values()), new Function<PublicationState, Integer>() {
          @Override
          public Integer apply(PublicationState input) {
            return input.constant;
          }
        });
  }

  private final Article article;
  private final ImmutableCollection<Syndication> syndications;

  private ArticleOutputView(Article article, Collection<Syndication> syndications) {
    this.article = Preconditions.checkNotNull(article);
    this.syndications = ImmutableList.copyOf(syndications);
  }

  public static ArticleOutputView create(Article article, SyndicationService syndicationService) {
    Collection<Syndication> syndications = null;
    try {
      syndications = syndicationService.getSyndications(article.getDoi());
    } catch (NoSuchArticleIdException e) {
      // Fall through and let it be set in the if-null block
    }
    if (syndications == null) {
      syndications = ImmutableList.of();
    }
    return new ArticleOutputView(article, syndications);
  }

  /**
   * Adapter object that produces the desired JSON output from a view instance.
   */
  public static final JsonSerializer<ArticleOutputView> SERIALIZER = new JsonSerializer<ArticleOutputView>() {
    @Override
    public JsonElement serialize(ArticleOutputView src, Type typeOfSrc, JsonSerializationContext context) {
      Article article = src.article;
      JsonObject serialized = new JsonObject();

      PublicationState pubState = PublicationState.BY_CONSTANT.get(article.getState());
      serialized.addProperty("doi", article.getDoi()); // Force it to be printed first, for human-friendliness
      serialized.addProperty("state", pubState.name().toLowerCase()); // Instead of the int value
      serialized.add("syndications", context.serialize(src.syndications));

      JsonObject baseJson = context.serialize(article).getAsJsonObject();
      serialized = JsonAdapterUtil.copyWithoutOverwriting(baseJson, serialized);

      return serialized;
    }
  };

}
