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

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Logs attributes of incoming HTTP requests.
 */
public class LoggingInterceptor extends HandlerInterceptorAdapter {

  private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug(describe(request));
    }
    return super.preHandle(request, response, handler);
  }

  private static String asStringLiteral(Object input) {
    String text = String.valueOf(input);
    if (input == null) {
      return text;
    }

    /*
     * TODO: Use org.apache.commons.lang.StringEscapeUtils?
     * StringEscapeUtils is a little funny with forward-slashes, which makes these values hard to read. So for now,
     * just handle some simple cases by hand. The returned values are not guaranteed to be parsable in any particular
     * context; this is just for human readability.
     */
    text = text.replace("\\", "\\\\").replace("\"", "\\\"");
    return '"' + text + '"';
  }

  private static final Joiner JOINER = Joiner.on(", ");
  private static final String INDENT = "  ";

  /**
   * Build a message describing a request.
   *
   * @param request the request to describe
   * @return the description
   */
  private static String describe(HttpServletRequest request) {
    StringBuilder message = new StringBuilder();
    message.append(request.getMethod()).append(' ').append(request.getRequestURI());
    String queryString = request.getQueryString();
    if (queryString != null) {
      message.append(" ? ").append(queryString);
    }
    message.append('\n');

    // Append a list of headers
    for (Enumeration headerNames = request.getHeaderNames(); headerNames.hasMoreElements(); ) {
      String headerName = (String) headerNames.nextElement();
      message.append(INDENT).append(asStringLiteral(headerName)).append(": ");
      Enumeration<?> headers = request.getHeaders(headerName);
      Iterator<String> headerStrings = Iterators.transform(Iterators.forEnumeration(headers), LoggingInterceptor::asStringLiteral);
      JOINER.appendTo(message, headerStrings);
      message.append('\n');
    }

    return message.toString();
  }

}
