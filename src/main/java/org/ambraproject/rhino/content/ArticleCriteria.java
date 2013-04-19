package org.ambraproject.rhino.content;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.rest.RestClientException;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.Collection;
import java.util.List;
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


  /**
   * Fetch a list of DOIs of articles in the system that match this object's criteria.
   *
   * @param hibernateTemplate the system's Hibernate template
   * @return a list of article DOIs
   */
  public List<String> apply(HibernateTemplate hibernateTemplate) {
    Preconditions.checkNotNull(hibernateTemplate);
    if (syndicationStatuses.isPresent()) {
      // TODO Handle this case
    }

    DetachedCriteria criteria = DetachedCriteria.forClass(Article.class);
    if (publicationStates.isPresent()) {
      criteria = criteria.add(Restrictions.in("state", publicationStates.get()));
    }
    List<?> result = hibernateTemplate.findByCriteria(criteria
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        .setProjection(Projections.property("doi"))
        .addOrder(Order.asc("lastModified")));
    return (List<String>) result;
  }

}
