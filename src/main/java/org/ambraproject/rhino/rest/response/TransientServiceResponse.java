package org.ambraproject.rhino.rest.response;

import com.google.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Objects;

/**
 * A response that does not have any timestamp data associated with it, and is reliable only at the time it is served.
 */
public final class TransientServiceResponse extends ServiceResponse {

  private final Object body;

  private TransientServiceResponse(HttpStatus status, Object body) {
    super(status);
    this.body = Objects.requireNonNull(body);
  }

  @Override
  Object getResponseBody() throws IOException {
    return body;
  }

  /**
   * Serve a view representing a newly created entity in a response.
   *
   * @param responseBody the view to serialize as the response
   * @return the response
   */
  public static TransientServiceResponse reportCreated(Object responseBody) {
    Objects.requireNonNull(responseBody);
    return new TransientServiceResponse(HttpStatus.CREATED, responseBody);
  }

  /**
   * Serve a view in a response.
   *
   * @param responseBody the view to serialize as the response
   * @return the response
   */
  public static TransientServiceResponse serveView(Object responseBody) {
    Objects.requireNonNull(responseBody);
    return new TransientServiceResponse(HttpStatus.OK, responseBody);
  }


  /**
   * Produce a response entity that represents this response to Spring.
   *
   * @param entityGson the service bean that produces JSON from view objects
   * @return the Spring representation of this response
   * @throws IOException
   */
  public ResponseEntity<?> asJsonResponse(Gson entityGson) throws IOException {
    Object entity = Objects.requireNonNull(getResponseBody());
    String json = entityGson.toJson(entity);
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(json);
  }

}
