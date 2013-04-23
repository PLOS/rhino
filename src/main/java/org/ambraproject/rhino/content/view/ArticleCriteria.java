package org.ambraproject.rhino.content.view;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.ambraproject.models.Article;
import org.ambraproject.models.Syndication;
import org.ambraproject.rhino.rest.RestClientException;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.ambraproject.rhino.content.view.ArticleJsonConstants.PUBLICATION_STATE_CONSTANTS;
import static org.ambraproject.rhino.content.view.ArticleJsonConstants.PUBLICATION_STATE_NAMES;
import static org.ambraproject.rhino.content.view.ArticleJsonConstants.SYNDICATION_STATUSES;

/**
 * Criteria from an API client describing a subset of articles.
 */
public class ArticleCriteria {

  private final Optional<ImmutableSet<Integer>> publicationStates;
  private final Optional<ImmutableSet<String>> syndicationStatuses;

  private ArticleCriteria(Optional<ImmutableSet<Integer>> publicationStates,
                          Optional<ImmutableSet<String>> syndicationStatuses) {
    this.publicationStates = Preconditions.checkNotNull(publicationStates);
    this.syndicationStatuses = Preconditions.checkNotNull(syndicationStatuses);
  }

  /**
   * Create an object describing a set of articles.
   *
   * @param clientPubStates    include all articles whose publication state is one of these; {@code null} to include all
   *                           articles regardless of publication state
   * @param clientSyndStatuses include all articles whose publication state is one of these; {@code null} to include all
   *                           articles regardless of publication state
   * @return
   */
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
        builder.add(clientSyndStatus);
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
  public Object apply(HibernateTemplate hibernateTemplate) {
    Preconditions.checkNotNull(hibernateTemplate);
    if (syndicationStatuses.isPresent()) {
      return findBySyndication(hibernateTemplate);
    }

    DetachedCriteria criteria = DetachedCriteria.forClass(Article.class);
    ProjectionList projectionList = Projections.projectionList().add(Projections.property("doi"));
    if (publicationStates.isPresent()) {
      criteria = criteria.add(Restrictions.in("state", publicationStates.get()));
      projectionList = projectionList.add(Projections.property("state"));
    }
    List<?> result = hibernateTemplate.findByCriteria(criteria
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        .setProjection(projectionList)
        .addOrder(Order.asc("lastModified")));
    return publicationStates.isPresent()
        ? new ArticleViewList(Lists.transform((List<Object[]>) result, DOI_AND_STATE_AS_VIEW))
        : new DoiList((List<String>) result);
  }

  private static final Function<Object[], ArticleView> DOI_AND_STATE_AS_VIEW = new Function<Object[], ArticleView>() {
    @Override
    public ArticleView apply(Object[] input) {
      String doi = (String) input[0];
      Integer pubStateConstant = (Integer) input[1];
      String pubStateName = ArticleJsonConstants.PUBLICATION_STATE_CONSTANTS.get(pubStateConstant);
      return new ArticleStateView(doi, pubStateName, null);
    }
  };


  // Optimization parameter; doesn't matter if it's off. Main use case is "CROSSREF" and "PMC".
  private static final int EXPECTED_SYNDICATION_TARGETS = 2;

  /*
   * Special-case hack requiring weird logic.
   */
  private ArticleViewList findBySyndication(HibernateTemplate hibernateTemplate) {
    List<Object[]> results = hibernateTemplate.execute(new HibernateCallback<List<Object[]>>() {
      @Override
      public List<Object[]> doInHibernate(Session session) throws HibernateException, SQLException {
        Query query = session.createQuery(SYND_QUERY);
        query.setParameterList("syndStatuses", syndicationStatuses.get());
        query.setParameterList("pubStates", publicationStates.or(PUBLICATION_STATE_CONSTANTS.keySet()));
        return query.list();
      }
    });

    List<ArticleStateView> views = Lists.newArrayListWithExpectedSize(results.size() / EXPECTED_SYNDICATION_TARGETS);
    ArticleStateViewBuilder builder = null;
    for (Object[] result : results) {
      String doi = (String) result[0];
      Integer pubStateConstant = (Integer) result[1];
      String pubStateName = ArticleJsonConstants.PUBLICATION_STATE_CONSTANTS.get(pubStateConstant);
      Syndication syndication = (Syndication) result[2];

      if (builder == null || !doi.equals(builder.doi)) {
        if (builder != null) {
          views.add(builder.build());
        }
        builder = new ArticleStateViewBuilder(doi, pubStateName);
      }
      builder.syndications.add(syndication);
    }

    return new ArticleViewList(views);
  }

  private static final String SYND_QUERY = ""
      + "select a.doi, a.state, s from Article a, Syndication s "
      + "where (a.doi = s.doi) and (s.status in (:syndStatuses)) and (a.state in (:pubStates)) "
      + "order by a.lastModified asc, a.doi asc";

  private static class ArticleStateViewBuilder {
    private final String doi;
    private final String state;
    private final List<Syndication> syndications;

    public ArticleStateViewBuilder(String doi, String state) {
      this.doi = Preconditions.checkNotNull(doi);
      this.state = Preconditions.checkNotNull(state);
      this.syndications = Lists.newArrayListWithExpectedSize(EXPECTED_SYNDICATION_TARGETS);
    }

    public ArticleStateView build() {
      return new ArticleStateView(doi, state, syndications);
    }
  }

}
