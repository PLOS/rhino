package org.ambraproject.rhino.content;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.rest.RestClientException;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.Set;

/**
 * Criteria from an API client describing a subset of articles.
 */
public class ArticleCriteria implements ArticleJson {

  private final Optional<ImmutableSet<Integer>> publicationStates;
  private final Optional<ImmutableSet<String>> syndicationStatuses;

  private ArticleCriteria(Optional<ImmutableSet<Integer>> publicationStates,
                          Optional<ImmutableSet<String>> syndicationStatuses) {
    this.publicationStates = Preconditions.checkNotNull(publicationStates);
    this.syndicationStatuses = Preconditions.checkNotNull(syndicationStatuses);
  }

  public static ArticleCriteria create(Collection<String> clientPubStates, Collection<String> clientSyndStatuses) {
    Optional<ImmutableSet<Integer>> publicationStateConstants;
    if (CollectionUtils.isEmpty(clientPubStates)) {
      publicationStateConstants = Optional.absent();
    } else {
      ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
      for (String clientPubState : clientPubStates) {
        Integer pubStateConstant = PUBLICATION_STATE_NAMES.get(clientPubState.toLowerCase());
        if (pubStateConstant == null) {
          throw unrecognizedInputs("publication state", clientPubStates, PUBLICATION_STATE_NAMES.keySet());
        }
        builder.add(pubStateConstant);
      }
      publicationStateConstants = Optional.of(builder.build());
    }

    Optional<ImmutableSet<String>> syndicationStatusConstants;
    if (CollectionUtils.isEmpty(clientSyndStatuses)) {
      syndicationStatusConstants = Optional.absent();
    } else {
      ImmutableSet.Builder<String> builder = ImmutableSet.builder();
      for (String clientSyndStatus : clientSyndStatuses) {
        clientSyndStatus = clientSyndStatus.toUpperCase();
        if (!SYNDICATION_STATUSES.contains(clientSyndStatus)) {
          throw unrecognizedInputs("syndication status", clientSyndStatuses, SYNDICATION_STATUSES);
        }
      }
      syndicationStatusConstants = Optional.of(builder.build());
    }

    return new ArticleCriteria(publicationStateConstants, syndicationStatusConstants);
  }

  /*
   * Put somewhere for reuse?
   */
  private static RestClientException unrecognizedInputs(String valueDescription,
                                                        Collection<?> inputValues,
                                                        Set<?> expectedValues) {
    Preconditions.checkNotNull(valueDescription);
    Preconditions.checkArgument(!expectedValues.isEmpty());

    Set<?> inputValueSet = (inputValues instanceof Set) ? (Set<?>) inputValues
        : ImmutableSet.copyOf(inputValues);
    Set<?> unrecognizedValues = Sets.difference(inputValueSet, expectedValues);

    String message = String.format("Unrecognized values for %s: %s. Expected: %s.",
        valueDescription, unrecognizedValues, expectedValues);
    return new RestClientException(message, HttpStatus.BAD_REQUEST);
  }


  public DetachedCriteria get() {
    DetachedCriteria criteria = DetachedCriteria.forClass(Article.class);

    if (publicationStates.isPresent()) {
      criteria = criteria.add(Restrictions.in("state", publicationStates.get()));
    }

    // TODO Apply syndicationStatuses
    // (Is this even possible/feasible with DetachedCriteria? It involves joining. Might prefer HQL.

    return criteria;
  }

}
