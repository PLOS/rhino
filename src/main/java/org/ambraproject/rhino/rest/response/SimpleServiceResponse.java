package org.ambraproject.rhino.rest.response;

import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Objects;

class SimpleServiceResponse extends ServiceResponse {

  private final Object body;

  SimpleServiceResponse(HttpStatus status, Object body) {
    super(status);
    this.body = Objects.requireNonNull(body);
  }

  @Override
  Object getResponseBody() throws IOException {
    return body;
  }

}
