package org.ambraproject.rhino.view.comment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Comment;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleVisibility;

import java.util.Objects;

/**
 * A view of an comment with no relationships to its parent or child comments.
 */
public class CommentNodeView implements JsonOutputView {

  private final Comment comment;
  private final CompetingInterestStatement competingInterestStatement;
  private final ArticleVisibility parentArticle;

  private CommentNodeView(Comment comment, CompetingInterestStatement competingInterestStatement,
                          ArticleVisibility parentArticle) {
    this.comment = Objects.requireNonNull(comment);
    this.competingInterestStatement = Objects.requireNonNull(competingInterestStatement);
    this.parentArticle = Objects.requireNonNull(parentArticle);
  }

  private CommentNodeView(Comment comment, CompetingInterestStatement competingInterestStatement) {
    this.comment = Objects.requireNonNull(comment);
    this.competingInterestStatement = Objects.requireNonNull(competingInterestStatement);
    this.parentArticle = null;
  }

  public static class Factory {
    private final CompetingInterestPolicy competingInterestPolicy;

    public Factory(RuntimeConfiguration runtimeConfiguration) {
      this.competingInterestPolicy = new CompetingInterestPolicy(runtimeConfiguration);
    }

    public CommentNodeView create(Comment comment, String articleDoi) {
      return new CommentNodeView(comment, competingInterestPolicy.createStatement(comment),
          ArticleVisibility.create(Doi.create(articleDoi)));
    }

    public CommentNodeView create(Comment comment) {
      return new CommentNodeView(comment, competingInterestPolicy.createStatement(comment));
    }

    public CommentFlagOutputView createFlagView(Flag commentFlag) {
      return new CommentFlagOutputView(commentFlag, create(commentFlag.getFlaggedComment()));
    }
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = CommentOutputView.serializeBase(context, comment, competingInterestStatement);
    serialized.add("parentArticle", context.serialize(parentArticle));
    return serialized;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CommentNodeView that = (CommentNodeView) o;

    if (comment != null ? !comment.equals(that.comment) : that.comment != null) return false;
    if (competingInterestStatement != null ? !competingInterestStatement.equals(that.competingInterestStatement) : that.competingInterestStatement != null)
      return false;
    return parentArticle != null ? parentArticle.equals(that.parentArticle) : that.parentArticle == null;

  }

  @Override
  public int hashCode() {
    int result = comment != null ? comment.hashCode() : 0;
    result = 31 * result + (competingInterestStatement != null ? competingInterestStatement.hashCode() : 0);
    result = 31 * result + (parentArticle != null ? parentArticle.hashCode() : 0);
    return result;
  }
}

