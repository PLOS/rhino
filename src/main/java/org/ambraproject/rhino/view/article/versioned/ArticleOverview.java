package org.ambraproject.rhino.view.article.versioned;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.ArticleTable;

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
