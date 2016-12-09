package org.ambraproject.rhino.view.article;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Objects;

public class SyndicationOutputView implements JsonOutputView {

  private final Syndication syndication;

  private SyndicationOutputView(Syndication syndication) {
    this.syndication = Objects.requireNonNull(syndication);
  }

  public static SyndicationOutputView createSyndicationView(Syndication syndication) {
    return new SyndicationOutputView(syndication);
  }

  static JsonObject serializeBase(JsonSerializationContext context, Syndication syndication) {
    return context.serialize(syndication).getAsJsonObject();
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    return serializeBase(context, syndication);
  }

}
