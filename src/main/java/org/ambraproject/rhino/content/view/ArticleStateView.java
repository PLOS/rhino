package org.ambraproject.rhino.content.view;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import edu.emory.mathcs.backport.java.util.Collections;
import org.ambraproject.models.Syndication;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Collection;

import static org.ambraproject.rhino.content.view.ArticleJsonConstants.MemberNames;

public class ArticleStateView implements ArticleView {

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

  public static final JsonSerializer<ArticleStateView> SERIALIZER = new JsonSerializer<ArticleStateView>() {
    @Override
    public JsonElement serialize(ArticleStateView src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject serialized = new JsonObject();
      serialized.addProperty(MemberNames.DOI, src.doi);
      serialized.addProperty(MemberNames.STATE, src.publicationState);
      serialized.add(MemberNames.SYNDICATIONS, ArticleOutputView.serializeSyndications(src.syndications, context));
      return serialized;
    }
  };

}
