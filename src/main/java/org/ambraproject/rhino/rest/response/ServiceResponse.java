package org.ambraproject.rhino.rest.response;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

/**
 * A response that does not have any timestamp data associated with it, and is reliable only at the time it is served.
 */
public class ServiceResponse<T> {

  private final HttpStatus status;
  private final T body;
  private final Instant lastModified;

  private ServiceResponse(HttpStatus status, T body, Instant lastModified) {
    this.status = Objects.requireNonNull(status);
    this.body = body;
    this.lastModified = lastModified;
    Preconditions.checkArgument(body != null || lastModified == null);
  }

  /**
   * Serve a view representing a newly created entity in a response.
   *
   * @param responseBody the view to serialize as the response
   * @return the response
   */
  public static <T> ServiceResponse<T> reportCreated(T responseBody) {
    Objects.requireNonNull(responseBody);
    return new ServiceResponse<T>(HttpStatus.CREATED, responseBody, null);
  }

  /**
   * Serve a view in a response.
   *
   * @param responseBody the view to serialize as the response
   * @return the response
   */
  public static <T> ServiceResponse<T> serveView(T responseBody) {
    Objects.requireNonNull(responseBody);
    return new ServiceResponse<T>(HttpStatus.OK, responseBody, null);
  }

  static <T> ServiceResponse<T> serveCacheableView(T responseBody, Instant lastModified) {
    Objects.requireNonNull(responseBody);
    Objects.requireNonNull(lastModified);
    return new ServiceResponse<T>(HttpStatus.OK, responseBody, lastModified);
  }

  static <T> ServiceResponse<T> reportNotModified(Instant lastModified) {
    Objects.requireNonNull(lastModified);
    return new ServiceResponse<T>(HttpStatus.NOT_MODIFIED, null, lastModified);
  }


  /**
   * Produce a response entity that represents this response to Spring.
   *
   * @param entityGson the service bean that produces JSON from view objects
   * @return the Spring representation of this response
   * @throws IOException
   */
  public ResponseEntity<?> asJsonResponse(Gson entityGson) throws IOException {
    ResponseEntity.BodyBuilder response = ResponseEntity.status(this.status)
        .contentType(MediaType.APPLICATION_JSON);
    if (lastModified != null) {
      response = response.lastModified(lastModified.toEpochMilli());
    }
    if (body != null) {
      String json = entityGson.toJson(body);
      return response.body(json);
    }
    return response.build();
  }

}
