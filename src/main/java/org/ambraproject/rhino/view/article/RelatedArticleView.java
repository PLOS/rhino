package org.ambraproject.rhino.view.article;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.ArticleAuthor;
import org.ambraproject.rhino.model.ArticleRelationship;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.List;

public class RelatedArticleView implements JsonOutputView, ArticleView {

  private final ArticleRelationship raw;
  private final Optional<String> title;
  private final ImmutableList<ArticleAuthor> authors;

  public RelatedArticleView(ArticleRelationship raw, String title, List<ArticleAuthor> authors) {
    this.raw = Preconditions.checkNotNull(raw);
    this.title = Optional.fromNullable(title);
    this.authors = (authors == null) ? ImmutableList.<ArticleAuthor>of() : ImmutableList.copyOf(authors);
  }

  @Override
  public String getDoi() {
    return raw.getOtherArticleDoi();
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject view = new JsonObject();
    view.addProperty("doi", raw.getOtherArticleDoi());

    if (title.isPresent()) {
      view.addProperty("title", title.get());
    }
    if (!authors.isEmpty()) {
      view.add("authors", context.serialize(authors));
    }

    JsonObject rawSerialized = context.serialize(raw).getAsJsonObject();
    rawSerialized.remove("otherArticleDoi");
    JsonAdapterUtil.copyWithoutOverwriting(rawSerialized, view);

    return view;
  }

}
