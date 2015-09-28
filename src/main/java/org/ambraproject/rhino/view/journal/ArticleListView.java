package org.ambraproject.rhino.view.journal;

import com.google.common.base.Preconditions;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.view.JsonOutputView;

public class ArticleListView implements JsonOutputView {

  private final ArticleListIdentity identity;
  private final String title;

  public ArticleListView(ArticleListIdentity articleListIdentity, String title) {
    this.identity = Preconditions.checkNotNull(articleListIdentity);
    this.title = Preconditions.checkNotNull(title);
  }

  public ArticleListIdentity getIdentity() {
    return identity;
  }

  public String getTitle() {
    return title;
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(identity).getAsJsonObject();
    serialized.addProperty("title", title);
    return serialized;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ArticleListView that = (ArticleListView) o;
    return identity.equals(that.identity) && title.equals(that.title);
  }

  @Override
  public int hashCode() {
    return 31 * identity.hashCode() + title.hashCode();
  }

}
