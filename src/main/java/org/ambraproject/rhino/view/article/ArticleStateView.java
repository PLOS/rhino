package org.ambraproject.rhino.view.article;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.view.JsonOutputView;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

/**
 * A view of an article containing only its stateful attributes that can be modified by the client directly. This is the
 * output analogue to {@link ArticleInputView}.
 */
public class ArticleStateView implements JsonOutputView, ArticleView {

  private final String doi;
  @Nullable
  private final String publicationState;
  @Nullable
  private final Collection<? extends Syndication> syndications;

  public ArticleStateView(String doi, String publicationState, Collection<? extends Syndication> syndications) {
    this.doi = Preconditions.checkNotNull(doi);
    this.publicationState = publicationState;
    this.syndications = (syndications == null) ? null : Collections.unmodifiableCollection(syndications);
  }

  @Override
  public String getDoi() {
    return doi;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty(ArticleJsonNames.DOI, doi);
    serialized.addProperty(ArticleJsonNames.STATE, publicationState);
    serialized.add(ArticleJsonNames.SYNDICATIONS, ArticleOutputView.serializeSyndications(syndications, context));
    return serialized;
  }

}
