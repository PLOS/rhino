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

public final class CacheableServiceResponse extends ServiceResponse {

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


  public ResponseEntity<?> asJsonResponse(Date ifModifiedSince, Gson entityGson) throws IOException {
    return asJsonResponse(ifModifiedSince == null ? null : ifModifiedSince.toInstant(), entityGson);
  }

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
