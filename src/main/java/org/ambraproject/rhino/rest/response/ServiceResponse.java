package org.ambraproject.rhino.rest.response;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Objects;

public abstract class ServiceResponse {

  protected final HttpStatus status;

  ServiceResponse(HttpStatus status) {
    this.status = Objects.requireNonNull(status);
  }


  abstract Object getResponseBody() throws IOException;


  /**
   * @deprecated For unit tests only.
   */
  @VisibleForTesting
  @Deprecated
  public String readJson(Gson entityGson) {
    Object entity;
    try {
      entity = Objects.requireNonNull(getResponseBody());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return entityGson.toJson(entity);
  }

}
