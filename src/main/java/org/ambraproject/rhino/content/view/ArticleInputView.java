package org.ambraproject.rhino.content.view;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Type;
import java.util.Map;

import static org.ambraproject.rhino.content.view.ArticleJsonConstants.MemberNames;
import static org.ambraproject.rhino.content.view.ArticleJsonConstants.PUBLICATION_STATE_CONSTANTS;
import static org.ambraproject.rhino.content.view.ArticleJsonConstants.PUBLICATION_STATE_NAMES;
import static org.ambraproject.rhino.content.view.ArticleJsonConstants.SYNDICATION_STATUSES;

/**
 * A view of an update to an article's state as submitted by a REST client.
 */
public class ArticleInputView {

  public static class SyndicationUpdate {
    private final String target;
    private final String status;

    private SyndicationUpdate(String target, String status) {
      Preconditions.checkArgument(StringUtils.isNotBlank(target));
      Preconditions.checkArgument(SYNDICATION_STATUSES.contains(status));
      this.target = target;
      this.status = status;
    }

    public String getTarget() {
      return target;
    }

    public String getStatus() {
      return status;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SyndicationUpdate that = (SyndicationUpdate) o;
      return status.equals(that.status) && target.equals(that.target);
    }

    @Override
    public int hashCode() {
      return 31 * target.hashCode() + status.hashCode();
    }

    @Override
    public String toString() {
      return "SyndicationUpdate{" + "status='" + status + '\'' + ", target='" + target + '\'' + '}';
    }
  }


  private final Optional<Integer> publicationState;
  private final ImmutableMap<String, SyndicationUpdate> syndicationUpdates;

  private ArticleInputView(Integer publicationState, Map<String, SyndicationUpdate> syndicationUpdates) {
    Preconditions.checkArgument(publicationState == null || PUBLICATION_STATE_CONSTANTS.containsKey(publicationState));
    this.publicationState = Optional.fromNullable(publicationState);
    this.syndicationUpdates = (syndicationUpdates == null)
        ? ImmutableMap.<String, SyndicationUpdate>of()
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
   * article.
   *
   * @return the set of syndication status updates
   */
  public ImmutableList<SyndicationUpdate> getSyndicationUpdates() {
    return syndicationUpdates.values().asList();
  }

  @VisibleForTesting
  public SyndicationUpdate getSyndicationUpdate(String target) {
    return syndicationUpdates.get(target);
  }

  public static final JsonDeserializer<ArticleInputView> DESERIALIZER = new JsonDeserializer<ArticleInputView>() {
    @Override
    public ArticleInputView deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject jsonObject = json.getAsJsonObject();
      Integer pubStateConstant = getPublicationState(jsonObject);
      Map<String, SyndicationUpdate> syndicationUpdates = getSyndicationUpdates(jsonObject);
      return new ArticleInputView(pubStateConstant, syndicationUpdates);
    }

    private Map<String, SyndicationUpdate> getSyndicationUpdates(JsonObject jsonObject) {
      JsonElement syndicationsObject = jsonObject.get(MemberNames.SYNDICATIONS);
      if (syndicationsObject == null) {
        return null;
      }

      Map<String, SyndicationUpdate> syndicationUpdateMap = Maps.newLinkedHashMap();
      for (Map.Entry<String, JsonElement> entry : syndicationsObject.getAsJsonObject().entrySet()) {
        String target = entry.getKey();
        String status = entry.getValue().getAsJsonObject().get(MemberNames.SYNDICATION_STATUS).getAsJsonPrimitive().getAsString();
        status = status.toUpperCase();
        if (!SYNDICATION_STATUSES.contains(status)) {
          throw new JsonParseException("Not a valid syndication status: " + status);
        }

        SyndicationUpdate update = new SyndicationUpdate(target, status);
        SyndicationUpdate previous = syndicationUpdateMap.put(target, update);
        if (previous != null && !previous.getStatus().equals(status)) {
          String message = String.format("Multiple statuses submitted for %s: %s, %s",
              target, previous.getStatus(), status);
          throw new JsonParseException(message);
        }
      }
      return syndicationUpdateMap;
    }

    private Integer getPublicationState(JsonObject jsonObject) {
      JsonElement stateValue = jsonObject.get(MemberNames.STATE);
      if (stateValue == null) {
        return null;
      }

      String pubStateName = stateValue.getAsJsonPrimitive().getAsString();
      pubStateName = pubStateName.toLowerCase();
      Integer pubStateConstant = PUBLICATION_STATE_NAMES.get(pubStateName);
      if (pubStateConstant == null) {
        String message = String.format("Unrecognized publication state: \"%s\". Expected one of: %s",
            pubStateName, PUBLICATION_STATE_NAMES.keySet());
        throw new JsonParseException(message);
      }
      return pubStateConstant;
    }
  };


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleInputView that = (ArticleInputView) o;

    if (!publicationState.equals(that.publicationState)) return false;
    if (!syndicationUpdates.equals(that.syndicationUpdates)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + publicationState.hashCode();
    result = 31 * result + syndicationUpdates.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "ArticleInputView{"
        + "publicationState=" + publicationState
        + ", syndicationUpdates=" + syndicationUpdates + '}';
  }
}
