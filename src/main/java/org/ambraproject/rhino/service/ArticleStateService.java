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

import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.view.article.ArticleInputView;

import java.io.IOException;

/**
 * Service that reads and writes an article's state after it has been created.
 */
public interface ArticleStateService {

  /**
   * Writes a set of client inputs to the persistent article identified by articleId.
   *
   * @param articleId identifies the article whose state we are updating
   * @param input     the client-submitted values to update
   * @return the article after updates are applied
   */
  public abstract Article update(ArticleIdentity articleId, ArticleInputView input)
      throws FileStoreException, IOException;
}
