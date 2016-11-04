package org.ambraproject.rhino.rest.response;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Objects;

public abstract class ServiceResponse {

  private final HttpStatus status;

  ServiceResponse(HttpStatus status) {
    this.status = Objects.requireNonNull(status);
  }


  public static ServiceResponse reportCreated(Object responseBody) {
    Objects.requireNonNull(responseBody);
    return new SimpleServiceResponse(HttpStatus.CREATED, responseBody);
  }

  public static ServiceResponse serveView(Object responseBody) {
    Objects.requireNonNull(responseBody);
    return new SimpleServiceResponse(HttpStatus.OK, responseBody);
  }


  abstract Object getResponseBody() throws IOException;

  public ResponseEntity<?> asJsonResponse(Gson entityGson) throws IOException {
    Object entity = Objects.requireNonNull(getResponseBody());
    String json = entityGson.toJson(entity);
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(json);
  }

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
