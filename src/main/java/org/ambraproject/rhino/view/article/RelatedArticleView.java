package org.ambraproject.rhino.view.article;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleRelationship;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Objects;
import java.util.Optional;

public class RelatedArticleView implements JsonOutputView, ArticleView {

  private final ArticleRelationship raw;
  private final Optional<Article> relatedArticle;

  public RelatedArticleView(ArticleRelationship rawRelationship, Article relatedArticle) {
    if (relatedArticle != null) {
      Preconditions.checkArgument(rawRelationship.getOtherArticleID().equals(relatedArticle.getID()));
      Preconditions.checkArgument(rawRelationship.getOtherArticleDoi().equals(relatedArticle.getDoi()));
    }

    this.raw = Objects.requireNonNull(rawRelationship);
    this.relatedArticle = Optional.ofNullable(relatedArticle);
  }

  @Override
  public String getDoi() {
    return raw.getOtherArticleDoi();
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject view = context.serialize(raw).getAsJsonObject();
    view.add("doi", view.remove("otherArticleDoi"));
    relatedArticle.ifPresent((Article relatedArticle) -> {
      view.add("title", context.serialize(relatedArticle.getTitle()));
      view.add("authors", context.serialize(relatedArticle.getAuthors()));
      view.add("collaborativeAuthors", context.serialize(relatedArticle.getCollaborativeAuthors()));
      view.add("date", context.serialize(relatedArticle.getDate()));
    });
    return view;
  }

}
