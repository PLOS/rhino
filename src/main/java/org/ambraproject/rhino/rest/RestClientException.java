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

package org.ambraproject.rhino.rest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * An error to be represented to a RESTful client with an appropriate HTTP status code and a message in the (plain text)
 * response body.
 */
public class RestClientException extends RuntimeException {

  private static final Logger log = LoggerFactory.getLogger(RestClientException.class);

  private final HttpStatus responseStatus;

  public RestClientException(String message, HttpStatus responseStatus) {
    super(message);
    this.responseStatus = responseStatus;
    validate();
  }

  public RestClientException(String message, HttpStatus responseStatus, Throwable cause) {
    super(message, cause);
    this.responseStatus = responseStatus;
    validate();
  }

  /**
   * Log warnings if the arguments to this exception are invalid. (Throwing an IllegalArgumentException would be
   * counterproductive, as it would only obscure this exception.)
   */
  private void validate() {
    if (responseStatus == null) {
      log.warn("Null response status");
    } else {
      int responseCode = responseStatus.value();
      if (responseCode < 400 || responseCode >= 500) {
        log.warn("HTTP status ({}) should report a client error (400-499)", responseCode);
      }
    }

    if (StringUtils.isBlank(getMessage())) {
      log.warn("No message (required to form the response body)");
    }
  }

  public HttpStatus getResponseStatus() {
    return responseStatus;
  }

}
