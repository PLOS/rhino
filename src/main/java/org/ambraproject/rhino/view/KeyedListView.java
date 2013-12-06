package org.ambraproject.rhino.view;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import java.util.Collection;
import java.util.Collections;

/**
 * An output view of objects, which presents those objects as members of one big JSON object.
 *
 * @param <T> the value type to represent
 */
public abstract class KeyedListView<T> implements JsonOutputView {

  private final Collection<T> values; // may contain nulls -- Hibernate quirk?

  public KeyedListView(Collection<? extends T> values) {
    this.values = Collections.unmodifiableCollection(values);
  }

  /**
   * Derive the JSON member name from a value.
   *
   * @param value the value
   * @return the value's JSON member name
   */
  protected abstract String getKey(T value);

  /**
   * Represent a value in a serializable view. By default, just use the value as the view.
   *
   * @param value the object to represent
   * @return the view
   */
  protected Object wrap(T value) {
    return value;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serializedList = new JsonObject();
    for (T value : values) {
      if (value == null) continue;
      String key = Preconditions.checkNotNull(getKey(value));
      if (serializedList.has(key)) {
        throw new IllegalStateException("Collision on key: " + key);
      }
      Object view = wrap(value);
      serializedList.add(key, context.serialize(view));
    }
    return serializedList;
  }

}
