package org.ambraproject.rhino.content;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * A view of an update to an article's state as submitted by a REST client.
 */
public class ArticleInputView implements ArticleJson {

  private final Optional<Integer> publicationState;
  private final ImmutableMap<String, String> syndicationUpdates;

  private ArticleInputView(Integer publicationState, Map<String, String> syndicationUpdates) {
    Preconditions.checkArgument(publicationState == null || PUBLICATION_STATE_CONSTANTS.containsKey(publicationState));
    this.publicationState = Optional.fromNullable(publicationState);
    this.syndicationUpdates = (syndicationUpdates == null)
        ? ImmutableMap.<String, String>of()
        : ImmutableMap.copyOf(syndicationUpdates);
  }

  /**
   * Get the article's new publication state, represented by an integer constant as established in {@link
   * org.ambraproject.models.Article}. If absent, indicates that the state is not changed.
   *
   * @return the publication state
   */
  public Optional<Integer> getPublicationState() {
    return publicationState;
  }

  /**
   * Get the set of updates to status fields of {@link org.ambraproject.models.Syndication} objects associated with the
   * article. The map's keys are syndication targets and the values are the new status values. An empty map indicates no
   * updates.
   *
   * @return the set of syndication status updates
   */
  public ImmutableMap<String, String> getSyndicationUpdates() {
    return syndicationUpdates;
  }

  public static final JsonDeserializer<ArticleInputView> DESERIALIZER = new JsonDeserializer<ArticleInputView>() {
    @Override
    public ArticleInputView deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject jsonObject = json.getAsJsonObject();

      Integer pubStateConstant = null;
      JsonElement stateValue = jsonObject.get(MemberNames.STATE);
      if (stateValue != null) {
        String pubStateName = stateValue.getAsJsonPrimitive().getAsString();
        pubStateName = pubStateName.toLowerCase();
        pubStateConstant = PUBLICATION_STATE_NAMES.get(pubStateName);
      }

      Map<String, String> syndicationUpdates = null;
      JsonElement syndicationsObject = jsonObject.get(MemberNames.SYNDICATIONS);
      if (syndicationsObject != null) {
        syndicationUpdates = Maps.newLinkedHashMap();
        for (Map.Entry<String, JsonElement> entry : syndicationsObject.getAsJsonObject().entrySet()) {
          String target = entry.getKey();
          String status = entry.getValue().getAsJsonObject().get(MemberNames.SYNDICATION_STATUS).getAsJsonPrimitive().getAsString();
          status = status.toUpperCase();
          if (!SYNDICATION_STATUSES.contains(status)) {
            throw new JsonParseException("Not a valid syndication status: " + status);
          }
          syndicationUpdates.put(target, status);
        }
      }

      return new ArticleInputView(pubStateConstant, syndicationUpdates);
    }
  };

}
