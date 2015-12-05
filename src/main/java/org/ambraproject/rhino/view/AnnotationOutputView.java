package org.ambraproject.rhino.view;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Annotation;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.view.article.ArticleVisibility;

import java.util.Collection;
import java.util.Comparator;
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

  private AnnotationOutputView(ArticleVisibility parentArticle,
                               Annotation comment,
                               List<AnnotationOutputView> replies) {
    this.parentArticle = Objects.requireNonNull(parentArticle);
    this.comment = Objects.requireNonNull(comment);
    this.replies = ImmutableList.copyOf(replies);
  }

  public static class Factory {
    private final ArticleVisibility parentArticle;
    private final Map<Long, List<Annotation>> commentsByParent;

    /**
     * @param parentArticle an article
     * @param comments      all comments belonging to the parent article
     */
    public Factory(Article parentArticle, Collection<Annotation> comments) {
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
      return new AnnotationOutputView(parentArticle, comment, childViews);
    }
  }

  public static final Comparator<Annotation> BY_DATE = Comparator.comparing(Annotation::getCreated);

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(comment).getAsJsonObject();
    serialized.remove("articleID");
    serialized.add("parentArticle", context.serialize(parentArticle));
    serialized.add("replies", context.serialize(replies));
    return serialized;
  }

}
