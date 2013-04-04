package org.ambraproject.rhino.content;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.ambraproject.models.Article;
import org.ambraproject.models.Syndication;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.service.article.NoSuchArticleIdException;
import org.ambraproject.service.syndication.SyndicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Collection;

/**
 * A view of an article for printing to JSON. When serialized to a JSON object, it should contain all the same fields as
 * a default dump of the article, plus a few added or augmented.
 */
public class ArticleOutputView {

  private static final Logger log = LoggerFactory.getLogger(ArticleOutputView.class);

  /**
   * The state constants given in {@link Article} mapped onto names chosen for this API.
   */
  private static final ImmutableBiMap<Integer, String> PUBLICATION_STATE_CONSTANTS = ImmutableBiMap.<Integer, String>builder()
      .put(Article.STATE_ACTIVE, "published")
      .put(Article.STATE_UNPUBLISHED, "ingested")
      .put(Article.STATE_DISABLED, "disabled")
      .build();
  private static final ImmutableBiMap<String, Integer> PUBLICATION_STATE_NAMES = PUBLICATION_STATE_CONSTANTS.inverse();

  private final Article article;
  private final ImmutableCollection<Syndication> syndications;

  private ArticleOutputView(Article article, Collection<Syndication> syndications) {
    this.article = Preconditions.checkNotNull(article);
    this.syndications = ImmutableList.copyOf(syndications);
  }

  public static ArticleOutputView create(Article article, SyndicationService syndicationService) {
    Collection<Syndication> syndications;
    try {
      syndications = syndicationService.getSyndications(article.getDoi());
    } catch (NoSuchArticleIdException e) {
      throw new RuntimeException(e); // should be impossible
    }
    if (syndications == null) {
      log.warn("SyndicationService.getSyndications returned null; assuming no syndications");
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
      serialized.addProperty("doi", article.getDoi()); // Force it to be printed first, for human-friendliness

      int articleState = article.getState();
      String pubState = PUBLICATION_STATE_CONSTANTS.get(articleState);
      if (pubState == null) {
        String message = String.format("Article.state field has unexpected value (%d). Expected one of: %s",
            articleState, PUBLICATION_STATE_CONSTANTS.keySet().toString());
        throw new IllegalStateException(message);
      }
      serialized.addProperty("state", pubState);

      serialized.add("syndications", serializeSyndications(src.syndications, context));

      JsonObject baseJson = context.serialize(article).getAsJsonObject();
      serialized = JsonAdapterUtil.copyWithoutOverwriting(baseJson, serialized);

      return serialized;
    }

    /**
     * Represent the list of syndications as an object whose keys are the syndication targets.
     */
    private JsonElement serializeSyndications(Collection<Syndication> syndications, JsonSerializationContext context) {
      JsonObject syndicationsByTarget = new JsonObject();
      for (Syndication syndication : syndications) {
        JsonObject syndicationJson = context.serialize(syndication).getAsJsonObject();

        // Exclude redundant members
        syndicationJson.remove("doi");
        syndicationJson.remove("target");

        syndicationsByTarget.add(syndication.getTarget(), syndicationJson);
      }
      return syndicationsByTarget;
    }

  };

}
