package org.ambraproject.rhino.view.user;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.view.JsonOutputView;

/**
 * Wrapper for an ID pointing into the user database.
 */
public class UserIdView implements JsonOutputView {

  private final long userProfileId;

  public UserIdView(long userProfileId) {
    this.userProfileId = userProfileId;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();

    /*
     * Although the userProfileId's low-level representation is a long integer, in the API we expose this essentially
     * non-numeric value as a string. This serves two purposes. First, it is general future-proofing in case we ever
     * want to change the ID scheme to allow arbitrary strings. Second, it helps prevent loss of precision in case the
     * client deserializes it to a floating-point value or smaller integer.
     */
    String userId = String.valueOf(userProfileId);

    serialized.addProperty("userId", userId);
    return serialized;
  }

  @Override
  public boolean equals(Object o) {
    return (this == o) || ((o != null) && (getClass() == o.getClass())
        && (userProfileId == ((UserIdView) o).userProfileId));
  }

  @Override
  public int hashCode() {
    return Long.hashCode(userProfileId);
  }

}
