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

package org.ambraproject.rhino.view.article;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class ArticleOverview {

  /**
   * The article's DOI.
   */
  private final String doi;

  /**
   * Each existing ingestion, mapped to the set of revision numbers that have been assigned to it. Many ingestions will
   * have an empty set of revisions. Typically an ingestion will have at most one revision; more than one revision is
   * possible, so we expose it in case the client has done something weird.
   * <p>
   * Don't refactor to use a {@link com.google.common.collect.Multimap}, because Multimaps consider keys with empty
   * collections not to exist and will leave them out of iteration.
   */
  private final ImmutableSortedMap<Integer, ImmutableSortedSet<Integer>> ingestions;

  /**
   * Each revision, mapped to the ingestion number to be displayed for it. Unlike {@link #ingestions}, each revision
   * points at exactly one revision.
   */
  private final ImmutableSortedMap<Integer, Integer> revisions;

  private ArticleOverview(ArticleIdentifier articleId,
                          Map<Integer, ImmutableSortedSet<Integer>> ingestionTable,
                          Map<Integer, Integer> revisionTable) {
    this.doi = articleId.getDoiName();
    this.ingestions = ImmutableSortedMap.copyOf(ingestionTable);
    this.revisions = ImmutableSortedMap.copyOf(revisionTable);
  }

  public static ArticleOverview build(ArticleIdentifier articleId,
                                      Collection<ArticleIngestion> ingestions,
                                      Collection<ArticleRevision> revisions) {
    // Initialize every ingestion number with an empty list of revisions, then fill in revisions.
    Map<Integer, Collection<Integer>> ingestionTable = ingestions.stream().collect(Collectors.toMap(
        ArticleIngestion::getIngestionNumber,
        ingestion -> new ArrayList<>(1)));
    for (ArticleRevision revision : revisions) {
      int ingestionKey = revision.getIngestion().getIngestionNumber();
      ingestionTable.get(ingestionKey).add(revision.getRevisionNumber());
    }

    Map<Integer, Integer> revisionTable = revisions.stream().collect(Collectors.toMap(
        ArticleRevision::getRevisionNumber,
        revision -> revision.getIngestion().getIngestionNumber()));

    return new ArticleOverview(articleId, Maps.transformValues(ingestionTable, ImmutableSortedSet::copyOf), revisionTable);
  }

}
