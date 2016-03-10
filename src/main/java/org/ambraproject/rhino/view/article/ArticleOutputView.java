/*
 * Copyright (c) 2006-2014 by Public Library of Science
 *
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambraproject.rhino.view.article;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Article;
import org.ambraproject.models.Category;
import org.ambraproject.models.Journal;
import org.ambraproject.models.Pingback;
import org.ambraproject.models.Syndication;
import org.ambraproject.rhino.service.ArticleType;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.KeyedListView;
import org.ambraproject.rhino.view.asset.groomed.GroomedAssetsView;
import org.ambraproject.rhino.view.asset.raw.RawAssetCollectionView;
import org.ambraproject.rhino.view.comment.CommentCount;
import org.ambraproject.rhino.view.journal.JournalNonAssocView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.ambraproject.rhino.view.article.ArticleJsonConstants.MemberNames;
import static org.ambraproject.rhino.view.article.ArticleJsonConstants.PUBLICATION_STATE_CONSTANTS;
import static org.ambraproject.rhino.view.article.ArticleJsonConstants.getPublicationStateName;

/**
 * A view of an article for printing to JSON.
 */
public class ArticleOutputView implements JsonOutputView, ArticleView {

  private static final Logger log = LoggerFactory.getLogger(ArticleOutputView.class);

  // The base view contains only the data that Hibernate ordinarily puts into an Article model object
  private final Article article;
  private final boolean excludeCitations;

  private ArticleOutputView(Article article, boolean excludeCitations) {
    this.article = Preconditions.checkNotNull(article);
    this.excludeCitations = excludeCitations;
  }

  public static ArticleOutputView createMinimalView(Article article) {
    return new ArticleOutputView(article, true);
  }

  /**
   * Data in addition to the Article object that can be provided by an {@link org.ambraproject.rhino.view.article.ArticleOutputViewFactory}.
   */
  public static class AugmentedView extends ArticleOutputView {
    private final Optional<String> nlmArticleType;
    private final Optional<ArticleType> articleType;
    private final ImmutableList<RelatedArticleView> relatedArticles;
    private final ImmutableList<ArticleIssue> articleIssues;
    private final ImmutableMap<String, Syndication> syndications;
    private final ImmutableList<Pingback> pingbacks;
    private final CommentCount commentCount;

    // Package-private; should be called only by ArticleOutputViewFactory
    AugmentedView(Article article,
                  String nlmArticleType,
                  ArticleType articleType,
                  Collection<RelatedArticleView> relatedArticles,
                  Collection<ArticleIssue> articleIssues,
                  Collection<Syndication> syndications,
                  Collection<Pingback> pingbacks,
                  CommentCount commentCount,
                  boolean excludeCitations) {
      super(article, excludeCitations);
      this.nlmArticleType = Optional.fromNullable(nlmArticleType);
      this.articleType = Optional.fromNullable(articleType);
      this.relatedArticles = ImmutableList.copyOf(relatedArticles);
      this.articleIssues = ImmutableList.copyOf(articleIssues);
      this.syndications = Maps.uniqueIndex(syndications, Syndication::getTarget);
      this.pingbacks = ImmutableList.copyOf(pingbacks);
      this.commentCount = commentCount;
    }

    @Override
    public Syndication getSyndication(String target) {
      return syndications.get(target);
    }

    @Override
    protected void augment(JsonSerializationContext context, JsonObject serialized) {
      if (nlmArticleType.isPresent()) {
        serialized.addProperty("nlmArticleType", nlmArticleType.get());
      }
      if (articleType.isPresent()) {
        serialized.add("articleType", context.serialize(articleType.get()));
      }

      serialized.add("commentCount", context.serialize(commentCount));

      JsonElement syndications = serializeSyndications(this.syndications.values(), context);
      if (syndications != null) {
        serialized.add(MemberNames.SYNDICATIONS, syndications);
      }
      serializePingbackDigest(serialized, context);

      KeyedListView<ArticleIssue> articleIssuesView = ArticleIssueOutputView.wrapList(articleIssues);
      serialized.add("issues", context.serialize(articleIssuesView));

      serialized.add("relatedArticles", context.serialize(relatedArticles));

      serialized.add(MemberNames.PINGBACKS, context.serialize(pingbacks));
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
    return null;
  }

  protected void augment(JsonSerializationContext context, JsonObject serialized) {
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty(MemberNames.DOI, article.getDoi()); // Force it to be printed first, for human-friendliness

    int articleState = article.getState();
    String pubState = getPublicationStateName(articleState);
    if (pubState == null) {
      String message = String.format("Article.state field has unexpected value (%d). Expected one of: %s",
          articleState, PUBLICATION_STATE_CONSTANTS);
      throw new IllegalStateException(message);
    }
    serialized.addProperty(MemberNames.STATE, pubState);

    Set<Journal> journals = article.getJournals();
    KeyedListView<Journal> journalsView = JournalNonAssocView.wrapList(journals);
    serialized.add("journals", context.serialize(journalsView));

    serialized.addProperty("pageCount", parsePageCount(article.getPages()));
    serialized.add("categories", context.serialize(buildCategoryViews(article.getCategories())));

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

    augment(context, serialized);

    serialized = JsonAdapterUtil.copyWithoutOverwriting(baseJson, serialized);

    /*
     * Because it is so bulky, put the raw asset view at the bottom (overwriting the default serialization of the
     * native article.assets field) for better human readability.
     */
    serialized.remove("assets"); // otherwise the new value would be inserted in the old position
    serialized.add("assets", context.serialize(new RawAssetCollectionView(article)));

    return serialized;
  }

  private static Collection<CategoryView> buildCategoryViews(Map<Category, Integer> categoryMap) {
    Collection<CategoryView> categoryViews = Lists.newArrayListWithCapacity(categoryMap.size());
    for (Map.Entry<Category, Integer> entry : categoryMap.entrySet()) {
      Category category = JsonAdapterUtil.forceHibernateLazyLoad(entry.getKey(), Category.class);
      int weight = entry.getValue();
      categoryViews.add(new CategoryView(category, weight));
    }
    return categoryViews;
  }

  private static final Pattern PAGE_PATTERN = Pattern.compile("(\\d+)-(\\d+)");

  private static int parsePageCount(String pages) {
    if (Strings.isNullOrEmpty(pages)) return 0;
    Matcher matcher = PAGE_PATTERN.matcher(pages);
    if (matcher.matches()) {
      try {
        int start = Integer.parseInt(matcher.group(1));
        int end = Integer.parseInt(matcher.group(2));
        return end - start + 1;
      } catch (NumberFormatException e) {
        // In case of integer overflow. Fall through.
      }
    }
    log.error("Article page range could not be parsed: {}", pages);
    return 0;
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
