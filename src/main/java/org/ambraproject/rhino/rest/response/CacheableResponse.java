/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.rest.response;

import org.ambraproject.rhino.model.Timestamped;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A response that has a timestamp showing the last-modified time of the represented data, which can be sent to the
 * client for caching and compared against an "If-Modified-Since" header.
 */
public final class CacheableResponse<T> {

  private final Supplier<? extends T> supplier;
  private final Instant lastModified;

  private CacheableResponse(Instant lastModified, Supplier<? extends T> supplier) {
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
  public static <T> CacheableResponse<T> serveView(Instant lastModified, Supplier<? extends T> supplier) {
    return new CacheableResponse<>(lastModified, supplier);
  }

  /**
   * Serve a view representing a timestamped entity.
   *
   * @param entity       the entity to represent in the response
   * @param viewFunction a function that converts the entity into a serializable view
   * @param <E>          the entity's type
   * @param <T>          the view's type
   * @return a response of the view
   */
  public static <T, E extends Timestamped> CacheableResponse<T>
  serveEntity(E entity, Function<? super E, ? extends T> viewFunction) {
    Objects.requireNonNull(viewFunction);
    Supplier<T> supplier = () -> viewFunction.apply(entity);
    Instant lastModified = entity.getLastModified().toInstant();
    return new CacheableResponse<>(lastModified, supplier);
  }


  public ServiceResponse<T> getIfModified(Date ifModifiedSince) throws IOException {
    return getIfModified(ifModifiedSince == null ? null : ifModifiedSince.toInstant());
  }

  /**
   * Compare against an "If-Modified-Since" header provided by the client and unpack into a {@link ServiceResponse} that
   * will return a "Not-Modified" status if applicable. The unpacked response also will provide a "Last-Modified"
   * timestamp.
   *
   * @param ifModifiedSince the timestamp provided by the requests's "If-Modified-Since" header, or {@code null} if the
   *                        request had no "If-Modified-Since" header
   * @return the unpacked response
   * @throws IOException
   */
  public ServiceResponse<T> getIfModified(Instant ifModifiedSince) throws IOException {
    if (ifModifiedSince != null && !ifModifiedSince.isBefore(lastModified)) {
      return ServiceResponse.reportNotModified(lastModified);
    }
    T body = Objects.requireNonNull(supplier.get());
    return ServiceResponse.serveCacheableView(body, lastModified);
  }

}
