package org.ambraproject.rhino.view;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Annotation;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.view.article.ArticleVisibility;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * View of a comment, with nested views of all its children.
 */
public class AnnotationOutputView implements JsonOutputView {

  private final ArticleVisibility parentArticle;
  private final Annotation comment;
  private final ImmutableList<AnnotationOutputView> replies;

  private final int replyTreeSize;
  private final Date mostRecentActivity;

  private final Date competingInterestThreshold;

  private AnnotationOutputView(ArticleVisibility parentArticle,
                               Annotation comment,
                               List<AnnotationOutputView> replies,
                               int replyTreeSize, Date mostRecentActivity,
                               Date competingInterestThreshold) {
    this.parentArticle = Objects.requireNonNull(parentArticle);
    this.comment = Objects.requireNonNull(comment);
    this.replies = ImmutableList.copyOf(replies);

    Preconditions.checkArgument(replyTreeSize >= 0);
    this.replyTreeSize = replyTreeSize;
    this.mostRecentActivity = Objects.requireNonNull(mostRecentActivity);

    this.competingInterestThreshold = Objects.requireNonNull(competingInterestThreshold);
  }

  public static class Factory {
    private final ArticleVisibility parentArticle;
    private final Map<Long, List<Annotation>> commentsByParent;

    private final Date competingInterestThreshold;

    /**
     * @param parentArticle an article
     * @param comments      all comments belonging to the parent article
     */
    public Factory(RuntimeConfiguration runtimeConfiguration, Article parentArticle, Collection<Annotation> comments) {
      this.competingInterestThreshold = Date.from(runtimeConfiguration.getCompetingInterestThreshold()
          .atStartOfDay(ZoneId.systemDefault()).toInstant());
      this.parentArticle = ArticleVisibility.create(parentArticle);
      this.commentsByParent = comments.stream()
          .filter(comment -> comment.getParentID() != null)
          .collect(Collectors.groupingBy(Annotation::getParentID));
    }

    /**
     * @param comment a comment belonging to this object's parent article
     * @return a view of the comment and all its children
     */
    public AnnotationOutputView buildView(Annotation comment) {
      List<Annotation> childObjects = commentsByParent.getOrDefault(comment.getID(), ImmutableList.of());
      List<AnnotationOutputView> childViews = childObjects.stream()
          .sorted(BY_DATE)
          .map(this::buildView) // recursion (terminal case is when childObjects is empty)
          .collect(Collectors.toList());

      int replyTreeSize = calculateReplyTreeSize(childViews);
      Date mostRecentActivity = findMostRecentActivity(comment, childViews);

      return new AnnotationOutputView(parentArticle, comment, childViews, replyTreeSize, mostRecentActivity,
          competingInterestThreshold);
    }

    private static int calculateReplyTreeSize(Collection<AnnotationOutputView> childViews) {
      return childViews.size() + childViews.stream().mapToInt(view -> view.replyTreeSize).sum();
    }

    private static Date findMostRecentActivity(Annotation comment, Collection<AnnotationOutputView> childViews) {
      return childViews.stream()
          .map(view -> view.mostRecentActivity)
          .max(Comparator.naturalOrder())
          .orElse(comment.getCreated());
    }
  }

  public static final Comparator<Annotation> BY_DATE = Comparator.comparing(Annotation::getCreated);


  /**
   * If the named field is null or absent, replace it with an empty string.
   */
  private static void normalizeField(JsonObject object, String name) {
    JsonElement element = object.get(name);
    if (element == null || element.isJsonNull()) {
      object.add(name, EMPTY_STRING);
    }
  }

  private static final JsonElement EMPTY_STRING = new JsonPrimitive("");

  /**
   * Check whether the competing interest statement should be suppressed.
   * <p>
   * An empty competing interest statement indicates that the user actively indicated that they had no competing
   * interests at the time the comment was created. If the comment was created before the competing interests feature
   * was implemented, then the user was silent. Indicate the difference by deleting the value if needed.
   */
  private boolean competingInterestShouldBeSuppressed() {
    boolean commentCreatedBeforeThreshold = comment.getCreated().compareTo(competingInterestThreshold) < 0;
    return commentCreatedBeforeThreshold && Strings.isNullOrEmpty(comment.getCompetingInterestBody());
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(comment).getAsJsonObject();
    normalizeField(serialized, "title");
    normalizeField(serialized, "body");
    normalizeField(serialized, "highlightedText");
    normalizeField(serialized, "competingInterestBody");
    if (competingInterestShouldBeSuppressed()) {
      serialized.remove("competingInterestBody");
    }

    serialized.remove("articleID");
    serialized.add("parentArticle", context.serialize(parentArticle));
    serialized.add("replyTreeSize", context.serialize(replyTreeSize));
    serialized.add("mostRecentActivity", context.serialize(mostRecentActivity));
    serialized.add("replies", context.serialize(replies));
    return serialized;
  }

}
