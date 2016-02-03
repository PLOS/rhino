package org.ambraproject.rhino.view;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.ambraproject.models.UserProfile;

import java.lang.reflect.Type;

/**
 * Suppresses all data from the user profile table except for the primary key, which is represented as the NED ID.
 * <p>
 * This will prevent any downstream service from consuming any data represented by the {@link UserProfile} model, in
 * preparation to move that data from this stack's persistence tier to NED.
 */
public enum UserProfileSerializer implements JsonSerializer<UserProfile> {
  INSTANCE;

  @Override
  public JsonElement serialize(UserProfile src, Type typeOfSrc, JsonSerializationContext context) {
    long id = src.getID();
    String idString = Long.toString(id); // prevent loss of precision, in case JSON is deserialized to floating point
    JsonObject view = new JsonObject();
    view.addProperty("nedId", idString);
    return view;
  }
}
