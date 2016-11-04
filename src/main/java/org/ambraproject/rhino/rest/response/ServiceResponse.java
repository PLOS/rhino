package org.ambraproject.rhino.rest.response;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Objects;

public class ServiceResponse {

  @FunctionalInterface
  public static interface ResponseSupplier {
    public abstract Object get() throws IOException;
  }

  private final ResponseSupplier supplier;
  private final HttpStatus status;

  ServiceResponse(ResponseSupplier supplier, HttpStatus status) {
    this.supplier = Objects.requireNonNull(supplier);
    this.status = Objects.requireNonNull(status);
  }


  public static ServiceResponse reportCreated(Object responseBody) {
    Objects.requireNonNull(responseBody);
    return new ServiceResponse(() -> responseBody, HttpStatus.CREATED);
  }

  public static ServiceResponse serveView(Object responseBody) {
    Objects.requireNonNull(responseBody);
    return new ServiceResponse(() -> responseBody, HttpStatus.OK);
  }


  public ResponseEntity<?> asJsonResponse(Gson entityGson) throws IOException {
    Object entity = Objects.requireNonNull(supplier.get());
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
      entity = Objects.requireNonNull(supplier.get());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return entityGson.toJson(entity);
  }

}
