package org.ambraproject.rhino.view;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.ambraproject.models.UserProfile;

import java.lang.reflect.Type;

/**
 * Minimal view of the {@link UserProfile} object, which exposes only those details that are needed to identify a user
 * and display external links to their user profile page. Avoids exposing any more data in anticipation of changes to
 * the data source.
 * <p>
 * As a bonus, prevents some lazy-loading complications.
 */
public enum UserProfileSerializer implements JsonSerializer<UserProfile> {
  INSTANCE;

  @Override
  public JsonElement serialize(UserProfile src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("authId", src.getAuthId());
    serialized.addProperty("displayName", src.getDisplayName());
    return serialized;
  }

}
