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

import org.ambraproject.rhino.util.response.Transceiver;

import java.io.IOException;
import java.util.List;

/**
 * Service class that deals with the hierarchy of taxonomic terms as they relate to the articles in a given journal.
 */
// I really wanted to call this TaxonomyService.  Unfortunately, the implementation delegates
// to org.ambraproject.service.taxonomy.TaxonomyService right now, and I thought it would just
// be too confusing to have two service classes that share the same names.
public interface ClassificationService {

  /**
   * Forwards the child terms of a given taxonomic subject area to the receiver, along with the counts of children for
   * each child.
   *
   * @param journal journal key specifying the journal
   * @param parent  the parent subject category that we will return children for.  If null or empty, the root of the
   *                hierarchy will be used.
   * @throws IOException
   */
  Transceiver read(String journal, String parent) throws IOException;

  void flagArticleCategory(Long articleId, Long categoryId, String authId) throws IOException;

  void deflagArticleCategory(Long articleId, Long categoryId, String authId) throws IOException;

}
