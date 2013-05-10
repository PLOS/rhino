package org.ambraproject.rhino.view;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.util.Collection;

public abstract class KeyedSingletonListView<T> implements JsonOutputView {

  private final String singletonMemberName;
  private final ImmutableList<T> values;

  protected KeyedSingletonListView(String singletonMemberName, Collection<? extends T> values) {
    this.singletonMemberName = Preconditions.checkNotNull(singletonMemberName);
    this.values = ImmutableList.copyOf(values);
  }

  protected abstract String getKey(T value);

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject list = new JsonObject();
    for (T value : values) {
      String key = getKey(value);
      JsonObject serializedValue = new JsonObject();
      serializedValue.add(singletonMemberName, context.serialize(value));
      list.add(key, serializedValue);
    }
    return list;
  }

}
