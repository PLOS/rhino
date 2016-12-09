package org.ambraproject.rhino.rest.response;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Objects;

/**
 * An encapsulated view to be returned from the controller layer.
 * <p>
 * Instances are generally returned from service interfaces in order to wrap around a view that can be serialized into
 * JSON by {@link Gson}. It also encapsulates the HTTP response status, generally either reporting "OK" or, from
 * services that persist data, "Created". A "Not-Modified" status can be served via the {@link CacheableResponse}
 * class.
 */
public class ServiceResponse<T> {

  private final HttpStatus status;
  private final T body;
  private final Instant lastModified;

  /**
   * Note that only this constructor allows null arguments as a private implementation detail. Each of the public and
   * package-private factory methods require non-null arguments.
   *
   * @param status       the status (non-nullable)
   * @param body         the response body, or {@code null} if this response is indicating a cache hit
   * @param lastModified the "Last-Modified" timestamp, or {@code null} if the service is not cacheable
   */
  private ServiceResponse(HttpStatus status, T body, Instant lastModified) {
    this.status = Objects.requireNonNull(status);
    this.body = body;
    this.lastModified = lastModified;
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

  /**
   * Serve a view as a response from a cacheable service. Indicates a cache miss.
   *
   * @param responseBody the view to serialize as the response
   * @param lastModified the timestamp at which the represented entity was last modified
   * @return the response
   */
  static <T> ServiceResponse<T> serveCacheableView(T responseBody, Instant lastModified) {
    Objects.requireNonNull(responseBody);
    Objects.requireNonNull(lastModified);
    return new ServiceResponse<T>(HttpStatus.OK, responseBody, lastModified);
  }

  /**
   * Service a response from a cacheable service indicating that the cached value has not changed.
   *
   * @param lastModified the timestamp at which the represented entity was last modified
   * @return the response
   */
  static <T> ServiceResponse<T> reportNotModified(Instant lastModified) {
    Objects.requireNonNull(lastModified);
    return new ServiceResponse<T>(HttpStatus.NOT_MODIFIED, null, lastModified);
  }


  private static final Charset JSON_CHARSET = Charsets.UTF_8;
  private static final MediaType JSON_TYPE = new MediaType("application", "json", JSON_CHARSET);

  /**
   * Produce a response entity that represents this response to Spring.
   * <p>
   * Implementation note: The return type is {@code ResponseEntity&lt;?>}, not {@code ResponseEntity&lt;T>}, because the
   * returned value might be a {@code ResponseEntity&lt;Void>} if this object was constructed from a {@link
   * CacheableResponse} where the request indicated a cache hit.
   *
   * @param entityGson the service bean that produces JSON from view objects
   * @return the Spring representation of this response
   * @throws IOException
   */
  public ResponseEntity<?> asJsonResponse(Gson entityGson) throws IOException {
    ResponseEntity.BodyBuilder response = ResponseEntity.status(this.status)
        .contentType(JSON_TYPE);
    if (lastModified != null) {
      response = response.lastModified(lastModified.toEpochMilli());
    }
    if (body != null) {
      String json = entityGson.toJson(body);
      return response.body(json.getBytes(JSON_CHARSET));
    }
    return response.build();
  }

}
