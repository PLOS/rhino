package org.ambraproject.rhino.view.comment;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Comment;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleTableVisibility;
import org.ambraproject.rhino.view.user.UserIdView;

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
public class CommentOutputView implements JsonOutputView {

  private final ArticleTableVisibility parentArticle;
  private final Comment comment;
  private final CompetingInterestStatement competingInterestStatement;

  private final ImmutableList<CommentOutputView> replies;
  private final int replyTreeSize;
  private final Date mostRecentActivity;

  private CommentOutputView(ArticleTableVisibility parentArticle,
                            Comment comment,
                            CompetingInterestStatement competingInterestStatement,
                            List<CommentOutputView> replies,
                            int replyTreeSize, Date mostRecentActivity) {
    this.parentArticle = Objects.requireNonNull(parentArticle);
    this.comment = Objects.requireNonNull(comment);
    this.competingInterestStatement = Objects.requireNonNull(competingInterestStatement);
    this.replies = ImmutableList.copyOf(replies);

    Preconditions.checkArgument(replyTreeSize >= 0);
    this.replyTreeSize = replyTreeSize;
    this.mostRecentActivity = Objects.requireNonNull(mostRecentActivity);
  }


  public static class Factory {
    private final CompetingInterestPolicy competingInterestPolicy;
    private final ArticleTableVisibility parentArticle;
    private final Map<Long, List<Comment>> commentsByParent;

    /**
     * @param comments      all comments belonging to the parent article
     */
    public Factory(RuntimeConfiguration runtimeConfiguration, List<Comment> comments) {
      this.competingInterestPolicy = new CompetingInterestPolicy(runtimeConfiguration);
      Doi doi = Doi.create(comments.get(0).getArticle().getDoi()); //todo: get(0) is unnecessary
      this.parentArticle = ArticleTableVisibility.create(doi);
      this.commentsByParent = comments.stream()
          .filter(comment -> comment.getParent() != null)
          .collect(Collectors.groupingBy(Comment::getParentId));
    }

    /**
     * @param comment a comment belonging to this object's parent article
     * @return a view of the comment and all its children
     */
    public CommentOutputView buildView(Comment comment) {
      List<Comment> childObjects = commentsByParent.getOrDefault(comment.getCommentId(), ImmutableList.of());
      List<CommentOutputView> childViews = childObjects.stream()
          .sorted(BY_DATE)
          .map(this::buildView) // recursion (terminal case is when childObjects is empty)
          .collect(Collectors.toList());

      CompetingInterestStatement competingInterestStatement = competingInterestPolicy.createStatement(comment);
      int replyTreeSize = calculateReplyTreeSize(childViews);
      Date mostRecentActivity = findMostRecentActivity(comment, childViews);

      return new CommentOutputView(parentArticle, comment, competingInterestStatement,
          childViews, replyTreeSize, mostRecentActivity);
    }

    private static int calculateReplyTreeSize(Collection<CommentOutputView> childViews) {
      return childViews.size() + childViews.stream().mapToInt(view -> view.replyTreeSize).sum();
    }

    private static Date findMostRecentActivity(Comment comment, Collection<CommentOutputView> childViews) {
      return childViews.stream()
          .map(view -> view.mostRecentActivity)
          .max(Comparator.naturalOrder())
          .orElse(comment.getCreated());
    }
  }

  public static final Comparator<Comment> BY_DATE = Comparator.comparing(Comment::getCreated);


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

  static JsonObject serializeBase(JsonSerializationContext context,
                                  Comment comment,
                                  CompetingInterestStatement competingInterestStatement) {
    JsonObject serialized = context.serialize(comment).getAsJsonObject();
    serialized.remove("userProfileID");

    serialized.add("creator", context.serialize(new UserIdView(comment.getUserProfileID())));

    serialized.remove("articleID");
    normalizeField(serialized, "title");
    normalizeField(serialized, "body");
    normalizeField(serialized, "highlightedText");

    serialized.remove("competingInterestBody");
    serialized.add("competingInterestStatement", context.serialize(competingInterestStatement));
    serialized.add("isRemoved", context.serialize(comment.getIsRemoved()));
    return serialized;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = serializeBase(context, comment, competingInterestStatement);

    serialized.add("parentArticle", context.serialize(parentArticle));
    serialized.add("replyTreeSize", context.serialize(replyTreeSize));
    serialized.add("mostRecentActivity", context.serialize(mostRecentActivity));
    serialized.add("replies", context.serialize(replies));
    return serialized;
  }

}
