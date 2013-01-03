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

package org.ambraproject.admin.rest;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Nullable;
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

  private static final Function<Object, String> STRING_LITERAL = new Function<Object, String>() {
    @Override
    public String apply(@Nullable Object input) {
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
  };

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
      message.append(INDENT).append(STRING_LITERAL.apply(headerName)).append(": ");
      Enumeration<?> headers = request.getHeaders(headerName);
      Iterator<String> headerStrings = Iterators.transform(Iterators.forEnumeration(headers), STRING_LITERAL);
      JOINER.appendTo(message, headerStrings);
      message.append('\n');
    }

    return message.toString();
  }

}
