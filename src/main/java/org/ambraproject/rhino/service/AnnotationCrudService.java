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
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.util.response.Transceiver;

import java.io.IOException;

/**
 * Service that handles comments and replies associated with articles. For legacy reasons, these are referred to
 * internally as "annotations," and they share a common implementation.
 */
public interface AnnotationCrudService {

  /**
   * Forwards all comments, and any replies associated with them, for a given article to the receiver.  The comments are
   * returned as a list.  Each comment has a "replies" list that contains any replies (recursively).
   *
   * @param articleIdentity identifies the article
   * @throws IOException
   */
  public Transceiver readComments(ArticleIdentity articleIdentity)
      throws IOException;

  /**
   * Reads the comment data for a single comment to the receiver.
   *
   * @param commentId identifies the comment
   * @throws IOException
   */
  public Transceiver readComment(DoiBasedIdentity commentId)
      throws IOException;

}
