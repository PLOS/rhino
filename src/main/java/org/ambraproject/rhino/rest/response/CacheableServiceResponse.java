package org.ambraproject.rhino.rest.response;

import com.google.gson.Gson;
import org.ambraproject.rhino.model.Timestamped;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;

/**
 * A response that has a timestamp showing the last-modified time of the represented data, which can be sent to the
 * client for caching and compared against an "If-Modified-Since" header.
 */
public final class CacheableServiceResponse extends ServiceResponse {

  /**
   * A function that lazily loads data to serve in a response body, in case the client does not have it cached.
   */
  @FunctionalInterface
  public static interface ResponseSupplier {
    public abstract Object get() throws IOException;
  }

  private final ResponseSupplier supplier;
  private final Instant lastModified;

  private CacheableServiceResponse(HttpStatus status, ResponseSupplier supplier, Instant lastModified) {
    super(status);
    this.supplier = Objects.requireNonNull(supplier);
    this.lastModified = Objects.requireNonNull(lastModified);
  }

  @Override
  Object getResponseBody() throws IOException {
    return supplier.get();
  }


  /**
   * Serve a view representing a piece of data with a particular timestamp.
   *
   * @param lastModified the data's timestamp
   * @param supplier     a function that will supply the data when invoked
   * @return the response
   */
  public static CacheableServiceResponse serveView(Instant lastModified, ResponseSupplier supplier) {
    return new CacheableServiceResponse(HttpStatus.OK, supplier, lastModified);
  }

  /**
   * Serve a view representing a timestamped entity.
   *
   * @param entity       the entity to represent in the response
   * @param viewFunction a function that converts the entity into a serializable view
   * @param <T>          the entity's type
   * @return the response
   */
  public static <T extends Timestamped> CacheableServiceResponse serveEntity(T entity,
                                                                             Function<? super T, ?> viewFunction) {
    Objects.requireNonNull(viewFunction);
    ResponseSupplier supplier = () -> viewFunction.apply(entity);
    Instant lastModified = entity.getLastModified().toInstant();
    return new CacheableServiceResponse(HttpStatus.OK, supplier, lastModified);
  }


  public ResponseEntity<?> asJsonResponse(Date ifModifiedSince, Gson entityGson) throws IOException {
    return asJsonResponse(ifModifiedSince == null ? null : ifModifiedSince.toInstant(), entityGson);
  }

  /**
   * Produce a response entity that represents this response to Spring and indicates to the client whether their cached
   * data is still up to date.
   *
   * @param ifModifiedSince the value of the "If-Modified-Since" header in the client's request, or {@code null} if the
   *                        client provided none
   * @param entityGson      the service bean that produces JSON from view objects
   * @return the Spring representation of this response, which has a "Not Modified" status if applicable
   * @throws IOException
   */
  public ResponseEntity<?> asJsonResponse(Instant ifModifiedSince, Gson entityGson) throws IOException {
    if (ifModifiedSince != null && !lastModified.isBefore(ifModifiedSince)) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }
    Object entity = Objects.requireNonNull(getResponseBody());
    String json = entityGson.toJson(entity);
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .lastModified(lastModified.toEpochMilli())
        .body(json);
  }

}
