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

package org.ambraproject.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

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

  private static String describe(HttpServletRequest request) {
    StringBuilder message = new StringBuilder();
    message.append(request.getMethod()).append(' ').append(request.getRequestURI());
    String queryString = request.getQueryString();
    if (queryString != null) {
      message.append(" ? ").append(queryString);
    }
    message.append('\n');
    for (Enumeration headerNames = request.getHeaderNames(); headerNames.hasMoreElements(); ) {
      String headerName = (String) headerNames.nextElement();
      String headerValue = request.getHeader(headerName);
      message.append("\t\"").append(headerName).append("\": \"").append(headerValue).append('\"').append('\n');
    }
    return message.toString();
  }

}
