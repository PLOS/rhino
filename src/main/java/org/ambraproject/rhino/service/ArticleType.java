package org.ambraproject.rhino.service;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.view.JsonOutputView;

import java.net.URI;

public class ArticleType implements JsonOutputView {

  private final URI uri;
  private final String heading;
  private final String pluralHeading;
  private final String code;

  public ArticleType(URI uri, String heading, String pluralHeading, String code) {
    this.uri = Preconditions.checkNotNull(uri);
    this.heading = Preconditions.checkNotNull(heading);
    this.pluralHeading = Preconditions.checkNotNull(pluralHeading);
    this.code = code; // nullable
  }

  public URI getUri() {
    return uri;
  }

  public String getHeading() {
    return heading;
  }

  public String getPluralHeading() {
    return pluralHeading;
  }

  public Optional<String> getCode() {
    return Optional.fromNullable(code);
  }


  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    // Suppress url
    serialized.addProperty("heading", heading);
    serialized.addProperty("pluralHeading", pluralHeading);
    serialized.addProperty("code", code);
    return serialized;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleType that = (ArticleType) o;

    if (!uri.equals(that.uri)) return false;
    if (!heading.equals(that.heading)) return false;
    if (!pluralHeading.equals(that.pluralHeading)) return false;
    if (code != null ? !code.equals(that.code) : that.code != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = uri.hashCode();
    result = 31 * result + heading.hashCode();
    result = 31 * result + pluralHeading.hashCode();
    result = 31 * result + (code != null ? code.hashCode() : 0);
    return result;
  }
}
