package org.ambraproject.rhino.rest.response;

import com.google.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Objects;

public final class TransientServiceResponse extends ServiceResponse {

  private final Object body;

  TransientServiceResponse(HttpStatus status, Object body) {
    super(status);
    this.body = Objects.requireNonNull(body);
  }

  @Override
  Object getResponseBody() throws IOException {
    return body;
  }

  public static TransientServiceResponse reportCreated(Object responseBody) {
    Objects.requireNonNull(responseBody);
    return new TransientServiceResponse(HttpStatus.CREATED, responseBody);
  }

  public static TransientServiceResponse serveView(Object responseBody) {
    Objects.requireNonNull(responseBody);
    return new TransientServiceResponse(HttpStatus.OK, responseBody);
  }


  public ResponseEntity<?> asJsonResponse(Gson entityGson) throws IOException {
    Object entity = Objects.requireNonNull(getResponseBody());
    String json = entityGson.toJson(entity);
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(json);
  }

}
