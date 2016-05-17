/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleIssue;
import org.ambraproject.rhino.view.journal.IssueInputView;
import org.ambraproject.rhino.view.journal.IssueOutputView;
import org.ambraproject.rhino.view.journal.VolumeNonAssocView;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IssueCrudServiceImpl extends AmbraService implements IssueCrudService {

  @Override
  public Transceiver read(final DoiBasedIdentity id) throws IOException {
    return new EntityTransceiver<Issue>() {
      @Override
      protected Issue fetchEntity() {
        Issue issue = (Issue) DataAccessUtils.uniqueResult((List<?>)
            hibernateTemplate.findByCriteria(DetachedCriteria
                    .forClass(Issue.class)
                    .add(Restrictions.eq("issueUri", id.getKey()))
            ));
        if (issue == null) {
          throw new RestClientException("Issue not found at URI=" + id.getIdentifier(), HttpStatus.NOT_FOUND);
        }
        return issue;
      }

      @Override
      protected Object getView(Issue issue) {
        return new IssueOutputView(issue, getParentVolume(issue));
      }
    };
  }

  private Issue applyInput(Issue issue, IssueInputView input) {
    String issueUri = input.getIssueUri();
    if (issueUri != null) {
      DoiBasedIdentity issueId = DoiBasedIdentity.create(issueUri);
      issue.setIssueUri(issueId.getKey());
    }

    String displayName = input.getDisplayName();
    if (displayName != null) {
      issue.setDisplayName(displayName);
    } else if (issue.getDisplayName() == null) {
      issue.setDisplayName("");
    }

    String imageUri = input.getImageUri();
    if (imageUri != null) {
      ArticleIdentity imageArticleId = ArticleIdentity.create(imageUri);
      if (!imageArticleId.getKey().equals(issue.getImageUri())) {
        updateImageArticle(issue, imageArticleId);
      }
    } else if (issue.getImageUri() == null) {
      issue.setImageUri("");
    }

    Boolean respectOrder = input.getRespectOrder();
    if (respectOrder != null) {
      issue.setRespectOrder(respectOrder);
    }

    List<String> inputArticleDois = input.getArticleOrder();
    if (inputArticleDois != null) {
      issue.setArticleDois(DoiBasedIdentity.asKeys(inputArticleDois));
    } else if (issue.getArticleDois() == null) {
      issue.setArticleDois(new ArrayList<String>(0));
    }

    return issue;
  }

  /**
   * Change an issue's image article. This also changes the issue's title and description to match those of the image
   * article.
   *
   * @param issue          the image to modify
   * @param imageArticleId the identity of the new image article
   * @throws RestClientException if {@code imageArticleId} doesn't match an existing article
   */
  private void updateImageArticle(Issue issue, ArticleIdentity imageArticleId) {
    Preconditions.checkNotNull(imageArticleId);
    Object[] result = (Object[]) DataAccessUtils.uniqueResult(hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Article.class)
            .add(Restrictions.eq("doi", imageArticleId.getKey()))
            .setProjection(Projections.projectionList()
                    .add(Projections.property("title"))
                    .add(Projections.property("description"))
            )
    ));
    if (result == null) {
      String message = "imageUri must belong to an existing article: " + imageArticleId.getIdentifier();
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
    String title = (String) result[0];
    String description = (String) result[1];

    issue.setImageUri(imageArticleId.getKey());
    issue.setTitle(title);
    issue.setDescription(description);
  }

  private Issue findIssue(DoiBasedIdentity issueId) {
    return (Issue) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
                .forClass(Issue.class)
                .add(Restrictions.eq("issueUri", issueId.getKey()))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
  }


  /**
   * Get a list of issues for a given article with associated journal and volume objects
   *
   * @param articleIdentity Article DOI that is contained in the Journal/Volume/Issue combinations which will be returned
   * @return a list of ArticleIssue objects that wrap each issue with its associated journal and volume objects
   */
  @Transactional(readOnly = true)
  @Override
  public List<ArticleIssue> getArticleIssues(final ArticleIdentity articleIdentity) {
    return (List<ArticleIssue>) hibernateTemplate.execute(new HibernateCallback() {
      public Object doInHibernate(Session session) throws HibernateException, SQLException {
        List<Object[]> queryResults = session.createSQLQuery(
                "select {j.*}, {v.*}, {i.*} " +
                        "from issueArticleList ial " +
                        "join issue i on ial.issueID = i.issueID " +
                        "join volume v on i.volumeID = v.volumeID " +
                        "join journal j on v.journalID = j.journalID " +
                        "where ial.doi = :articleURI " +
                        "order by i.created desc ")
                .addEntity("j", Journal.class)
                .addEntity("v", Volume.class)
                .addEntity("i", Issue.class)
                .setString("articleURI", articleIdentity.getKey())
                .list();

        List<ArticleIssue> articleIssues = new ArrayList<>(queryResults.size());
        for (Object[] row : queryResults) {
          Journal journal = (Journal) row[0];
          Volume volume = (Volume) row[1];
          Issue issue = (Issue) row[2];
          articleIssues.add(new ArticleIssue(issue, volume, journal));
        }

        return articleIssues;
      }
    });
  }

  @Override
  public DoiBasedIdentity create(DoiBasedIdentity volumeId, IssueInputView input) {
    Preconditions.checkNotNull(volumeId);

    DoiBasedIdentity issueId = DoiBasedIdentity.create(input.getIssueUri());
    if (findIssue(issueId) != null) {
      throw new RestClientException("Issue already exists with issueUri: " + issueId.getIdentifier(), HttpStatus.BAD_REQUEST);
    }

    Volume volume = (Volume) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
                .forClass(Volume.class)
                .add(Restrictions.eq("volumeUri", volumeId.getKey()))
                .setFetchMode("issues", FetchMode.JOIN)
        ));
    if (volume == null) {
      throw new RestClientException("Volume not found for volumeUri: " + volumeId.getIdentifier(), HttpStatus.BAD_REQUEST);
    }

    Issue issue = applyInput(new Issue(), input);
    List<Issue> volumeIssues = volume.getIssues();
    volumeIssues.add(issue);
    hibernateTemplate.update(volume);

    return DoiBasedIdentity.create(issue.getIssueUri());
  }

  @Override
  public void update(DoiBasedIdentity issueId, IssueInputView input) {
    Preconditions.checkNotNull(input);
    Issue issue = findIssue(issueId);
    if (issue == null) {
      throw new RestClientException("Issue not found for issueUri: " + issueId.getIdentifier(), HttpStatus.BAD_REQUEST);
    }
    issue = applyInput(issue, input);
    hibernateTemplate.update(issue);
  }

  @Override
  public VolumeNonAssocView getParentVolume(Issue issue) {
    Object[] parentVolumeResults = hibernateTemplate.execute((Session session) -> {
      String hql = "select volumeUri, displayName, imageUri, title, description " +
          "from Volume where :issue in elements(issues)";
      Query query = session.createQuery(hql);
      query.setParameter("issue", issue);
      return (Object[]) query.uniqueResult();
    });

    return (parentVolumeResults == null) ? null : new VolumeNonAssocView(
        (String) parentVolumeResults[0], (String) parentVolumeResults[1], (String) parentVolumeResults[2],
        (String) parentVolumeResults[3], (String) parentVolumeResults[4]);
  }

}
