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

package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.view.journal.ArticleListView;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface ArticleListCrudService {

  /**
   * Create a new article list.
   *
   * @param identity    the identity of the list to create
   * @param displayName the list's display name
   * @param articleIds  the non-empty set of articles that will display the link
   * @return the created list (augmented with its parent journal key)
   */
  ArticleListView create(ArticleListIdentity identity, String displayName, Set<ArticleIdentifier> articleIds);

  ArticleListView update(ArticleListIdentity identity, Optional<String> displayName,
                         Optional<? extends Set<ArticleIdentifier>> articleIds);

  ServiceResponse<ArticleListView> read(ArticleListIdentity identity);

  /**
   * @return the identities of all lists, excluding article metadata
   */
  ServiceResponse<Collection<ArticleListView>> readAll();

  /**
   * @param listType   the type of list to create
   * @param journalKey the journal the list belongs to
   * @return the identities of all lists, and all article metadata for all returned lists
   */
  ServiceResponse<Collection<ArticleListView>> readAll(String listType, Optional<String> journalKey);

  /**
   * Read all lists that contain the article.
   *
   * @param articleId the identity of an article
   * @return the identities of all lists that contain the article
   */
  ServiceResponse<Collection<ArticleListView>> readContainingLists(ArticleIdentifier articleId);

}
