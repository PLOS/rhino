package org.ambraproject.rhino.rest.response;

import com.google.gson.Gson;
import org.ambraproject.rhino.model.Timestamped;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CacheableServiceResponse extends ServiceResponse {

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


  public static CacheableServiceResponse serveView(Instant lastModified, ResponseSupplier supplier) {
    return new CacheableServiceResponse(HttpStatus.OK, supplier, lastModified);
  }

  public static <T extends Timestamped> CacheableServiceResponse serveEntity(T entity,
                                                                             Function<? super T, ?> viewFunction) {
    Objects.requireNonNull(viewFunction);
    ResponseSupplier supplier = () -> viewFunction.apply(entity);
    Instant lastModified1 = entity.getLastModified().toInstant();
    return new CacheableServiceResponse(HttpStatus.OK, supplier, lastModified1);
  }

  public static <T extends Timestamped> CacheableServiceResponse serveEntities(Collection<? extends T> entities,
                                                                               Function<? super T, ?> viewFunction) {
    Objects.requireNonNull(viewFunction);
    ResponseSupplier supplier = () -> entities.stream()
        .map(viewFunction)
        .collect(Collectors.toList());
    Instant lastModified = entities.stream()
        .map(entity -> entity.getLastModified().toInstant())
        .max(Comparator.naturalOrder()).orElse(Instant.MIN);
    return new CacheableServiceResponse(HttpStatus.OK, supplier, lastModified);
  }


  public ResponseEntity<?> asJsonResponse(Instant ifModifiedSince, Gson entityGson) throws IOException {
    if (lastModified.isAfter(ifModifiedSince)) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }
    Object entity = Objects.requireNonNull(getResponseBody());
    String json = entityGson.toJson(entity);
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .lastModified(getEpochMilliseconds(lastModified))
        .body(json);
  }

  private static long getEpochMilliseconds(Instant lastModified) {
    return lastModified.getEpochSecond() * 1000 + lastModified.getNano() / 1000;
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated Use {@link #asJsonResponse(Instant, Gson)} instead.
   */
  @Override
  @Deprecated
  public ResponseEntity<?> asJsonResponse(Gson entityGson) throws IOException {
    return super.asJsonResponse(entityGson);
  }

}
