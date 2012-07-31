/*
 * $HeadURL$
 * $Id$
 *
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

package org.ambraproject.admin;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.springframework.http.HttpStatus;

/**
 * An error to be represented to a RESTful client with an appropriate HTTP status code and a message in the (plain text)
 * response body.
 */
public class RestfulServerException extends RuntimeException {

  private final HttpStatus responseStatus;

  public RestfulServerException(String message, HttpStatus responseStatus) {
    super(Preconditions.checkNotNull(message));
    this.responseStatus = Preconditions.checkNotNull(responseStatus);
    checkResponseStatus();
  }

  public RestfulServerException(Throwable cause) {
    this(Strings.nullToEmpty(cause.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR, cause);
  }

  public RestfulServerException(String message, HttpStatus responseStatus, Throwable cause) {
    super(Preconditions.checkNotNull(message), cause);
    this.responseStatus = Preconditions.checkNotNull(responseStatus);
  }

  private void checkResponseStatus() {
    int statusCode = responseStatus.value();
    Preconditions.checkArgument(statusCode >= 400 && statusCode < 600,
        "HTTP status must indicate an error condition");
  }

  public HttpStatus getResponseStatus() {
    return responseStatus;
  }

}
