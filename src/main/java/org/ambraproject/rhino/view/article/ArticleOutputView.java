package org.ambraproject.rhino.view.article;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Article;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.models.Journal;
import org.ambraproject.models.Pingback;
import org.ambraproject.models.Syndication;
import org.ambraproject.rhino.service.PingbackReadService;
import org.ambraproject.rhino.shared.Rhino;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.KeyedListView;
import org.ambraproject.rhino.view.asset.groomed.GroomedAssetsView;
import org.ambraproject.rhino.view.asset.raw.RawAssetCollectionView;
import org.ambraproject.rhino.view.journal.JournalNonAssocView;
import org.ambraproject.service.article.NoSuchArticleIdException;
import org.ambraproject.service.syndication.SyndicationService;
import org.ambraproject.views.article.ArticleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.ambraproject.rhino.view.article.ArticleJsonConstants.MemberNames;
import static org.ambraproject.rhino.view.article.ArticleJsonConstants.PUBLICATION_STATE_CONSTANTS;
import static org.ambraproject.rhino.view.article.ArticleJsonConstants.getPublicationStateName;

/**
 * A view of an article for printing to JSON. When serialized to a JSON object, it should contain all the same fields as
 * a default dump of the article, plus a few added or augmented.
 */
public class ArticleOutputView implements JsonOutputView, ArticleView {

  private static final Logger log = LoggerFactory.getLogger(ArticleOutputView.class);

  private final Article article;
  private final ImmutableMap<String, Syndication> syndications;
  private final ImmutableList<Pingback> pingbacks;
  private final boolean excludeCitations;

  private ArticleOutputView(Article article, Collection<Syndication> syndications,
                            Collection<Pingback> pingbacks, boolean excludeCitations) {
    this.article = Preconditions.checkNotNull(article);
    this.syndications = Maps.uniqueIndex(syndications, GET_TARGET);
    this.pingbacks = ImmutableList.copyOf(pingbacks);
    this.excludeCitations = excludeCitations;
  }

  private static final Function<Syndication, String> GET_TARGET = new Function<Syndication, String>() {
    @Override
    public String apply(Syndication input) {
      return input.getTarget();
    }
  };

  /**
   * Creates a new view of the given article and associated data.
   *
   * @param article             primary entity
   * @param excludeCitations if true, don't serialize citation information
   * @param syndicationService
   * @param pingbackReadService   @return view of the article and associated data
   */
  public static ArticleOutputView create(Article article,
                                         boolean excludeCitations,
                                         SyndicationService syndicationService,
                                         PingbackReadService pingbackReadService) {
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
    List<Pingback> pingbacks = pingbackReadService.loadPingbacks(article);
    return new ArticleOutputView(article, syndications, pingbacks, excludeCitations);
  }

  @Override
  public String getDoi() {
    return article.getDoi();
  }

  @VisibleForTesting
  public Article getArticle() {
    return article;
  }

  @VisibleForTesting
  public Syndication getSyndication(String target) {
    return syndications.get(target);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty(MemberNames.DOI, article.getDoi()); // Force it to be printed first, for human-friendliness
    ArticleType articleType = Rhino.getKnownArticleType(article.getTypes());
    serialized.addProperty(MemberNames.ARTICLE_TYPE, articleType.getHeading());

    int articleState = article.getState();
    String pubState = getPublicationStateName(articleState);
    if (pubState == null) {
      String message = String.format("Article.state field has unexpected value (%d). Expected one of: %s",
          articleState, PUBLICATION_STATE_CONSTANTS);
      throw new IllegalStateException(message);
    }
    serialized.addProperty(MemberNames.STATE, pubState);

    JsonElement syndications = serializeSyndications(this.syndications.values(), context);
    if (syndications != null) {
      serialized.add(MemberNames.SYNDICATIONS, syndications);
    }
    serializePingbackDigest(serialized, context);

    Set<Journal> journals = article.getJournals();
    KeyedListView<Journal> journalsView = JournalNonAssocView.wrapList(journals);
    serialized.add("journals", context.serialize(journalsView));

    GroomedAssetsView groomedAssets = GroomedAssetsView.create(article);
    JsonAdapterUtil.copyWithoutOverwriting((JsonObject) context.serialize(groomedAssets), serialized);

    JsonObject baseJson;
    if (excludeCitations) {

      // The problem here is that serialize will call all getters, and some may be hibernate collections
      // that are lazily initialized.  For performance reasons, we sometimes don't want these lazy queries
      // to fire.  Since we have to decide whether to do this conditionally, this adapter approach seems
      // best (if we use an ExclusionStrategy it will apply globally).
      baseJson = context.serialize(new NoCitationsArticleAdapter(article)).getAsJsonObject();
    } else {
      baseJson = context.serialize(article).getAsJsonObject();
    }

    serialized = JsonAdapterUtil.copyWithoutOverwriting(baseJson, serialized);

    serialized.add(MemberNames.PINGBACKS, context.serialize(pingbacks));

    /*
     * Because it is so bulky, put the raw asset view at the bottom (overwriting the default serialization of the
     * native article.assets field) for better human readability.
     */
    serialized.remove("assets"); // otherwise the new value would be inserted in the old position
    serialized.add("assets", context.serialize(new RawAssetCollectionView(article)));

    return serialized;
  }

  /**
   * Add fields summarizing pingback data. These are redundant to the explicit list of pingback objects, but include
   * them anyway so that ArticlePingbackView is a subset of ArticleOutputView.
   *
   * @param serialized the JSON object to modify by adding pingback digest values
   * @param context    the JSON context
   * @return the modified object
   */
  private JsonObject serializePingbackDigest(JsonObject serialized, JsonSerializationContext context) {
    // These two fields are redundant, but include them so that ArticlePingbackView is a subset of this.
    serialized.addProperty("pingbackCount", pingbacks.size());
    if (!pingbacks.isEmpty()) {
      // The service guarantees that the list is ordered by timestamp
      Date mostRecent = pingbacks.get(0).getLastModified();
      serialized.add("mostRecentPingback", context.serialize(mostRecent));
    }
    return serialized;
  }

  /**
   * Represent the list of syndications as an object whose keys are the syndication targets.
   */
  static JsonElement serializeSyndications(Collection<? extends Syndication> syndications,
                                           JsonSerializationContext context) {
    if (syndications == null) {
      return null;
    }
    JsonObject syndicationsByTarget = new JsonObject();
    for (Syndication syndication : syndications) {
      JsonObject syndicationJson = context.serialize(syndication).getAsJsonObject();
      syndicationsByTarget.add(syndication.getTarget(), syndicationJson);
    }
    return syndicationsByTarget;
  }

}
