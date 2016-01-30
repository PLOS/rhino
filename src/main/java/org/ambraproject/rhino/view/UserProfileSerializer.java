package org.ambraproject.rhino.view;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.ambraproject.models.UserProfile;

import java.lang.reflect.Type;

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
