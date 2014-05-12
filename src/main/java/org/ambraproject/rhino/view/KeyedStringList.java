package org.ambraproject.rhino.view;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * A view of a collection of bare strings, formatted (when output as JSON) as a dictionary of objects. Each string
 * becomes one object, with the string itself as the object's only value.
 */
public abstract class KeyedStringList {

  private final Collection<String> strings;

  protected KeyedStringList(Collection<String> strings) {
    this.strings = Collections.unmodifiableCollection(strings);
  }

  /**
   * Transform a string in the collection into the object's key value (for the big dictionary that lists all objects).
   *
   * @param value the string
   * @return the key
   */
  protected abstract String extractIdentifier(String value);

  /**
   * Return the constant key that is used in the one key-value pair in each serialized object.
   *
   * @return the constant key string
   */
  protected abstract String getMemberName();

  @VisibleForTesting
  public Collection<String> getStrings() {
    return strings;
  }

  /*
   * Prefer TypeAdapter over JsonSerializer, because TypeAdapter streams efficiently over a lazy-loaded list.
   */
  public static final TypeAdapter<KeyedStringList> ADAPTER = new TypeAdapter<KeyedStringList>() {
    @Override
    public void write(JsonWriter out, KeyedStringList value) throws IOException {
      out.beginObject();
      for (String string : value.strings) {
        out.name(value.extractIdentifier(string));
        out.beginObject();
        out.name(value.getMemberName());
        out.value(string);
        out.endObject();
      }
      out.endObject();
    }

    @Deprecated
    @Override
    public KeyedStringList read(JsonReader in) throws IOException {
      throw new RuntimeException("Unsupported");
    }
  };

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return strings.equals(((KeyedStringList) o).strings);
  }

  @Override
  public int hashCode() {
    return strings.hashCode();
  }
}
