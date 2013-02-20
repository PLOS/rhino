/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Service that reads and writes an article's state after it has been created.
 */
public interface ArticleStateService {

  /**
   * Writes an article's state to the response.
   *
   * @param response HttpServletResponse for the current request
   * @param articleId wraps the article's DOI
   * @param format the desired metadata format
   * @throws IOException
   */
  void read(HttpServletResponse response, ArticleIdentity articleId, MetadataFormat format)
      throws IOException;
}
