/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.soa.rest;

import org.apache.commons.lang.StringUtils;
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
