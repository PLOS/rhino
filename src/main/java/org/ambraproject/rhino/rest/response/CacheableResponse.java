package org.ambraproject.rhino.rest.response;

import org.ambraproject.rhino.model.Timestamped;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;

/**
 * A response that has a timestamp showing the last-modified time of the represented data, which can be sent to the
 * client for caching and compared against an "If-Modified-Since" header.
 */
public final class CacheableResponse<T> {

  /**
   * A function that lazily loads data to serve in a response body, in case the client does not have it cached.
   */
  @FunctionalInterface
  public static interface ResponseSupplier<T> {
    public abstract T get() throws IOException;
  }

  private final ResponseSupplier<? extends T> supplier;
  private final Instant lastModified;

  private CacheableResponse(Instant lastModified, ResponseSupplier<? extends T> supplier) {
    this.supplier = Objects.requireNonNull(supplier);
    this.lastModified = Objects.requireNonNull(lastModified);
  }

  /**
   * Serve a view representing a piece of data with a particular timestamp.
   *
   * @param lastModified the data's timestamp
   * @param supplier     a function that will supply the data when invoked
   * @return the response
   */
  public static <T> CacheableResponse<T> serveView(Instant lastModified, ResponseSupplier<? extends T> supplier) {
    return new CacheableResponse<>(lastModified, supplier);
  }

  /**
   * Serve a view representing a timestamped entity.
   *
   * @param entity       the entity to represent in the response
   * @param viewFunction a function that converts the entity into a serializable view
   * @param <T>          the entity's type
   * @return the response
   */
  public static <T, E extends Timestamped> CacheableResponse<T>
  serveEntity(E entity, Function<? super E, ? extends T> viewFunction) {
    Objects.requireNonNull(viewFunction);
    ResponseSupplier<T> supplier = () -> viewFunction.apply(entity);
    Instant lastModified = entity.getLastModified().toInstant();
    return new CacheableResponse<>(lastModified, supplier);
  }


  public ServiceResponse<T> getIfModified(Date ifModifiedSince) throws IOException {
    return getIfModified(ifModifiedSince == null ? null : ifModifiedSince.toInstant());
  }

  public ServiceResponse<T> getIfModified(Instant ifModifiedSince) throws IOException {
    if ((ifModifiedSince != null) && (ifModifiedSince.compareTo(lastModified) <= 0)) {
      return ServiceResponse.reportNotModified(lastModified);
    } else {
      T body = Objects.requireNonNull(supplier.get());
      return ServiceResponse.serveCacheableView(body, lastModified);
    }
  }


}
