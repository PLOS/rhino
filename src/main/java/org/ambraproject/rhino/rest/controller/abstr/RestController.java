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

package org.ambraproject.rhino.rest.controller.abstr;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableListMultimap;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhombat.HttpDateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * Controller that sends HTTP responses to RESTful requests.
 */
public abstract class RestController {

  private static final Logger log = LoggerFactory.getLogger(RestController.class);

  @Autowired
  protected Gson entityGson;

  protected static final String ACCEPT_REQUEST_HEADER = "Accept";
  protected static final String JSONP_CALLBACK_PARAM = "callback";

  /**
   * Retrieve a RESTful argument that consists of the entire request URL after a namespace prefix. The namespace prefix
   * consists of one or more URI path elements that the API defines as a prefix for a class of RESTful "nouns" and must
   * end with a slash (for example, {@code "/article/"}). The request's URI must contain the given namespace prefix:
   * everything before the namespace (typically the path to the webapp, or nothing) is ignored, and everything after the
   * namespace is the return value.
   * <p/>
   * This is essentially doing the work of a {@link PathVariable} annotated parameter, but Spring does not seem to
   * support {@code PathVariable}s that span multiple URI components across slashes. So, if we want to treat a value
   * that contains slashes (such as a DOI, which is itself a nested URI) as a single variable, we have to parse it from
   * the request URI string ourselves.
   *
   * @param request   the HTTP request for a REST action
   * @param optional  if true, null will be returned if no additional path exists after the namespace; if false,
   *                  IllegalArgumentException will be thrown if this is the case
   * @param namespace the namespace in which the request was received  @return the contents of the request path after
   *                  the namespace prefix
   * @throws IllegalArgumentException if the request URI does not start with the namespace or if the namespace does not
   *                                  end with a slash, and optional is false
   */
  protected static String getFullPathVariable(HttpServletRequest request, boolean optional, final String namespace) {
    final int namespaceLength = namespace.length();
    if (namespaceLength < 3 || namespace.charAt(0) != '/' || namespace.charAt(namespaceLength - 1) != '/') {
      throw new IllegalArgumentException("Namespace must begin and end with '/'");
    }
    String requestUri = request.getRequestURI();
    int namespaceIndex = requestUri.indexOf(namespace);
    if (namespaceIndex < 0) {
      String message = String.format("Request URI (\"%s\") does not have expected namespace (\"%s\")",
          requestUri, namespace);
      throw new IllegalArgumentException(message);
    }
    int namespaceEnd = namespaceIndex + namespaceLength;
    int end = requestUri.length() - (requestUri.endsWith("/") ? 1 : 0);
    if (end <= namespaceEnd) {
      if (optional) {
        return null;
      } else {
        throw new IllegalArgumentException("Request URI has no path variable after namespace");
      }
    }
    return requestUri.substring(namespaceEnd, end);
  }

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
   * Return a plain-text HTTP response with an "OK" status code.
   *
   * @param text the response body
   * @return the response object
   */
  protected static ResponseEntity<String> respondWithPlainText(CharSequence text) {
    return respondWithPlainText(text, HttpStatus.OK);
  }

  /**
   * Return a JSON response with an "OK" status code. This should be used only for small objects; arbitrarily long JSON
   * responses should be streamed.
   *
   * @param value the object to serialize into the response body
   * @return the response object
   */
  protected ResponseEntity<String> respondWithJson(Object value) {
    String json = entityGson.toJson(value);
    HttpHeaders headers = makeContentTypeHeader(MediaType.APPLICATION_JSON);
    return new ResponseEntity<>(json, headers, HttpStatus.OK);
  }

  /**
   * Report that a RESTful operation succeeded. The returned object (if returned from a {@link RequestMapping}) will
   * cause the REST response to indicate an "OK" HTTP status and have an empty response body.
   *
   * @return a response indicating "OK"
   */
  protected ResponseEntity<Object> reportOk() {
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * Report that a RESTful operation to create an entity succeeded. The returned object (if returned from a {@link
   * RequestMapping}) will cause the REST response to indicate a "Created" HTTP status and have an empty response body.
   *
   * @return a response indicating "Created"
   */
  protected ResponseEntity<Object> reportCreated() {
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  /**
   * Report that a RESTful operation to create an entity succeeded. The returned object (if returned from a {@link
   * RequestMapping}) will cause the REST response to indicate a "Created" HTTP status and provide the identifier of the
   * created object to the client.
   *
   * @param identifier the identifier of the created object
   * @return a response indicating "Created"
   */
  protected ResponseEntity<String> reportCreated(String identifier) {
    return new ResponseEntity<>(identifier, HttpStatus.CREATED);
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
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
    calendar.setTime(lastModified);
    response.addHeader("Last-Modified", HttpDateUtil.format(calendar));
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
      Calendar headerCal = HttpDateUtil.parse(ifModifiedSince);
      Calendar lastModifiedCal = Calendar.getInstance();
      lastModifiedCal.setTimeZone(TimeZone.getTimeZone("GMT"));
      lastModifiedCal.setTime(lastModified);
      return lastModifiedCal.after(headerCal);
    }
  }
}
