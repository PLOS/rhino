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

import org.ambraproject.rhino.identity.ArticleIdentifier;

/**
 * Service that pushes messages to the queue related to Solr indexing.
 */
public interface SolrIndexService {

  /**
   * Push a message to the queue that will update an article's Solr index.
   *
   * @param articleId the article to be indexed
   */
  public abstract void updateSolrIndex(ArticleIdentifier articleId);

  /**
   * Push a message to the queue that will remove an article from the Solr index.
   *
   * @param articleId the article to be removed
   */
  public abstract void removeSolrIndex(ArticleIdentifier articleId);

}