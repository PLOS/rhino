package org.ambraproject.rhino.view.user;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.view.JsonOutputView;

public class UserIdView implements JsonOutputView {

  private final long userProfileId;

  public UserIdView(long userProfileId) {
    this.userProfileId = userProfileId;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("userId", String.valueOf(userProfileId));
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
