package org.ambraproject.rhino.view;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Annotation;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.view.article.ArticleVisibility;
import org.ambraproject.views.AnnotationView;

import java.util.List;
import java.util.Map;

public class AnnotationOutputView implements JsonOutputView {

  private final ArticleVisibility parentArticle;
  private final AnnotationView annotationView; // TODO Break dependence; redesign output API

  public AnnotationOutputView(Annotation annotation, Article article, Map<Long, List<Annotation>> replies) {
    this.parentArticle = ArticleVisibility.create(article);
    this.annotationView = new AnnotationView(annotation, article.getDoi(), article.getTitle(), replies);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(annotationView).getAsJsonObject();
    serialized.add("parentArticle", context.serialize(parentArticle));
    return serialized;
  }
}
