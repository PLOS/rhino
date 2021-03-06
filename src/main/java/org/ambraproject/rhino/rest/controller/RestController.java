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

package org.ambraproject.rhino.rest.controller;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableListMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.ambraproject.rhino.rest.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.apache.http.client.utils.DateUtils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;

/**
 * Controller that sends HTTP responses to RESTful requests.
 */
public abstract class RestController {

  private static final Logger log = LoggerFactory.getLogger(RestController.class);

  @Autowired
  protected Gson entityGson;


  /**
   * Produce an HTTP header set that defines the content type but no other information.
   *
   * @param mediaType the content type
   * @return the HTTP header set
   */
  protected static HttpHeaders makeContentTypeHeader(MediaType mediaType) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(mediaType);
    return headers;
  }

  /**
   * Return a response with a given status code, no body, and no defined headers.
   *
   * @param status the response status code
   * @return the response object
   */
  protected static <T> ResponseEntity<T> respondWithStatus(HttpStatus status) {
    return new ResponseEntity<>(status);
  }

  /**
   * Return a plain-text HTTP response.
   *
   * @param text   the response body
   * @param status the response's status code
   * @return the response object
   */
  private static ResponseEntity<String> respondWithPlainText(CharSequence text, HttpStatus status) {
    return new ResponseEntity<>(text.toString(), makeContentTypeHeader(MediaType.TEXT_PLAIN), status);
  }

  /**
   * Report that a RESTful operation to delete an object succeeded. The returned object (if returned from a {@link
   * RequestMapping}) will cause the REST response to indicate an "OK" HTTP status and have a response body identifying
   * the deleted object.
   *
   * @return a response indicating "OK"
   */
  protected ResponseEntity<Object> reportDeleted(String identifier) {
    return new ResponseEntity<>(identifier, HttpStatus.OK);
  }

  /**
   * Report an error condition to the REST client. The brief error message is sent as the response body, with the
   * response code specified when the exception object was created. The stack trace is not included because we generally
   * expect the client to fix the error with a simple change to input.
   *
   * @param e the exception that Spring wants to handle
   * @return the RESTful response body
   */
  @ExceptionHandler(RestClientException.class)
  public ResponseEntity<String> reportClientError(RestClientException e) {
    HttpStatus status = e.getResponseStatus();
    log.info("Reporting error to client (" + status + ")", e);
    String message = e.getMessage().trim() + '\n';
    return respondWithPlainText(message, status);
  }

  /**
   * Exception handler for exceptions thrown by the Spring MVC framework when incoming requests cannot be matched with
   * the @RequestParam-annotated parameters.  For example, if a request is missing a required parameter, or the types do
   * not match, this handler will be invoked.  This method returns a 405 status code to the client instead of a 500.
   *
   * @param srbe exception from the Spring framework
   * @return ResponseEntity encapsulating a 405 return code and an informative error message
   */
  @ExceptionHandler(ServletRequestBindingException.class)
  public ResponseEntity<String> reportRequestError(ServletRequestBindingException srbe) {
    return respondWithPlainText(srbe.getMessage(), HttpStatus.METHOD_NOT_ALLOWED);
  }

  /**
   * Display a server-side error to the rest client. This is meant generally to handle bugs and configuration errors.
   * Because this is assumed to be caused by programmer error, the stack trace is sent in the request body.
   *
   * @param e the exception that Spring wants to handle
   * @return the RESTful response body
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> reportServerError(Exception e) {
    log.error("Exception from controller", e);
    StringWriter report = new StringWriter();
    e.printStackTrace(new PrintWriter(report));

    return respondWithPlainText(report.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private static Reader readRequestBody(HttpServletRequest request) throws IOException {
    String characterEncoding = request.getCharacterEncoding();
    if (characterEncoding == null) {
      log.warn("Request with text body does not declare character encoding");
      return new InputStreamReader(request.getInputStream());
    }
    return new InputStreamReader(request.getInputStream(), characterEncoding);
  }

  /**
   * De-serializes a JSON object from an HTTP PUT request.  As a side effect of calling this method, the InputStream
   * associated with the request will be closed.
   *
   * @param request HttpServletRequest
   * @param clazz   class of the bean we are de-serializing
   * @param <T>     type of the bean we are de-serializing
   * @return JSON de-serialized to a bean
   * @throws IOException
   */
  protected <T> T readJsonFromRequest(HttpServletRequest request, Class<T> clazz) throws IOException {
    T result;
    try (Reader requestBody = readRequestBody(request)) {
      result = entityGson.fromJson(new BufferedReader(requestBody), clazz);
    } catch (JsonParseException e) {
      throw new RestClientException("Request body contains invalid JSON", HttpStatus.BAD_REQUEST, e);
    }
    if (result == null) {
      throw new RestClientException("Request body must contain JSON", HttpStatus.BAD_REQUEST);
    }

    return result;
  }

  protected static ImmutableListMultimap<String, String> getParameters(ServletRequest request) {
    Map<String, String[]> parameterMap = request.getParameterMap();
    if (parameterMap.isEmpty()) {
      return ImmutableListMultimap.of(); // avoid constructing a builder
    }
    ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
    for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
      String key = entry.getKey();
      String[] values = entry.getValue();
      for (int i = 0; i < values.length; i++) {
        builder.put(key, values[i]);
      }
    }
    return builder.build();
  }

  /**
   * Interpret a request parameter value as a boolean value. The argument should be a {@code String} supplied by a
   * {@link org.springframework.web.bind.annotation.RequestParam}-supplied value.
   * <p/>
   * The parameter is generally expected to be a flag-style URI parameter. For example, the client sends a request to
   * {@code http://example.com/api?foo} and the controller method has an argument {@code @RequestParam("foo") String
   * fooParam}. Then {@code fooParam} would have the value {@code ""}. If the client leaves {@code ?foo} off the URI,
   * then {@code fooParam} would be {@code null}. Hence, this method interprets {@code null} as {@code false} and
   * non-null strings in general, including the empty string, as {@code true}.
   * <p/>
   * However, the string {@code "false"} is (case-insensitively) interpreted as boolean {@code false}, so the client has
   * the option of sending to {@code http://example.com/api?foo=true} or {@code http://example.com/api?foo=false} and
   * getting sensible behavior. (This is especially handy for client scripts whose authors want to pass actual boolean
   * primitives, not strings, to their request library.)
   * <p/>
   * Contrast {@link Boolean#valueOf(String)}, which converts {@code null} and the empty string to {@code false}. Also
   * contrast {@code @RequestParam} on a {@code boolean} argument, which expects {@code "true"} or {@code "false"} as a
   * value and rejects other input.
   *
   * @param parameter a request parameter
   * @return the parameter interpreted as a boolean value
   */
  protected static boolean booleanParameter(String parameter) {
    return (parameter != null) && !Boolean.toString(false).equalsIgnoreCase(parameter);
  }

  /**
   * Sets the Last-Modified header of the response appropriately.
   *
   * @param response     HttpServletResponse
   * @param lastModified date to set the header to
   */
  protected void setLastModifiedHeader(HttpServletResponse response, Date lastModified) {
    response.addHeader("Last-Modified", DateUtils.formatDate(lastModified));
  }

  /**
   * Checks for the presence of an "If-Modified-Since" header on the request.  If it exists, returns true iff
   * lastModified is after the date in the header.  That is, returns true if we should send content, and false if a 304
   * Not Modified response is appropriate.
   *
   * @param request      HttpServletRequest
   * @param lastModified last modified date of the entity being requested
   * @return true if we should send the entity, false if we should send a 304 response
   */
  protected boolean checkIfModifiedSince(HttpServletRequest request, Date lastModified) {
    String ifModifiedSince = request.getHeader("If-Modified-Since");
    if (Strings.isNullOrEmpty(ifModifiedSince)) {
      return true;
    } else {
      Date ifModifiedSinceDate = DateUtils.parseDate(ifModifiedSince);
      return lastModified.after(ifModifiedSinceDate);
    }
  }
}
