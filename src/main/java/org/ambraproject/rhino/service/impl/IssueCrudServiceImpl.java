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
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.identity.IssueIdentifier;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.IssueArticle;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleIssue;
import org.ambraproject.rhino.view.journal.IssueInputView;
import org.ambraproject.rhino.view.journal.IssueOutputView;
import org.ambraproject.rhino.view.journal.VolumeNonAssocView;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("JpaQlInspection")
public class IssueCrudServiceImpl extends AmbraService implements IssueCrudService {

  @Autowired
  private VolumeCrudService volumeCrudService;

  @Autowired
  private ArticleCrudService articleCrudService;

  @Override
  public Issue getIssue(IssueIdentifier issueId) {
    Issue issue = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Issue WHERE issueUri = :issueUri");
      query.setParameter("issueUri", issueId.getIssueUri());
      return (Issue) query.uniqueResult();
    });
    if (issue == null) {
      throw new RestClientException("Issue not found with URI: " + issueId, HttpStatus.NOT_FOUND);
    }
    return issue;
  }

  @Override
  public List<IssueArticle> getIssueArticles(Issue issue) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM IssueArticle WHERE issueId = :issueId");
      query.setParameter("issueId", issue.getIssueId());
      return (List<IssueArticle>) query.list();
    });
  }

  @Override
  public List<ArticleTable> getArticles(Issue issue) {
    List<IssueArticle> issueArticles = getIssueArticles(issue);
    return issueArticles.stream().map(IssueArticle::getArticle).collect(Collectors.toList());
  }

  @Override
  public Transceiver read(final IssueIdentifier id) throws IOException {
    return new EntityTransceiver<Issue>() {
      @Override
      protected Issue fetchEntity() {
        return getIssue(id);
      }

      @Override
      protected Object getView(Issue issue) {
        return new IssueOutputView(issue, getParentVolumeView(issue), getArticles(issue));
      }
    };
  }

  private Issue applyInput(Issue issue, IssueInputView input) {
    String issueUri = input.getIssueUri(); //todo: pass in IssueIdentifier. Tackle during Volume update (DPRO-2716)
    if (issueUri != null) {
      IssueIdentifier issueId = IssueIdentifier.create(issueUri);
      issue.setIssueUri(issueId.getIssueUri());
    }

    String displayName = input.getDisplayName();
    if (displayName != null) {
      issue.setDisplayName(displayName);
    } else if (issue.getDisplayName() == null) {
      issue.setDisplayName("");
    }

    String imageUri = input.getImageUri();
    if (imageUri != null) {
      ArticleIdentifier imageArticleId = ArticleIdentifier.create(imageUri);
      if (!imageArticleId.getDoiName().equals(issue.getImageUri())) {
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
      List<IssueArticle> existingIssueArticles = getIssueArticles(issue);
      for (IssueArticle issueArticle : existingIssueArticles) {
        hibernateTemplate.delete(issueArticle);
      }

      List<ArticleTable> inputArticles = inputArticleDois.stream()
          .map(doi -> articleCrudService.getArticle(ArticleIdentifier.create(doi)))
          .collect(Collectors.toList());

      int sortOrder = 0;
      for (ArticleTable article : inputArticles) {
        IssueArticle issueArticle = new IssueArticle(issue, article, sortOrder);
        hibernateTemplate.save(issueArticle);
        sortOrder++;
      }
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
  private void updateImageArticle(Issue issue, ArticleIdentifier imageArticleId) {
    Preconditions.checkNotNull(imageArticleId);
    ArticleTable imageArticle = articleCrudService.getArticle(imageArticleId);
    if (imageArticle == null) {
      String message = "imageUri must belong to an existing article: " + imageArticleId;
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
    issue.setImageUri(imageArticleId.getDoiName());
    //todo: getters for article title and description
//    issue.setTitle(imageArticle.getTitle());
//    issue.setDescription(imageArticle.getDescription());
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
  //todo: use VolumeIdentifier instead of DoiBasedIdentity
  public DoiBasedIdentity create(DoiBasedIdentity volumeId, IssueInputView input) {
    Preconditions.checkNotNull(volumeId);

    IssueIdentifier issueId = IssueIdentifier.create(input.getIssueUri());
    if (getIssue(issueId) != null) {
      throw new RestClientException("Issue already exists with issueUri: " + issueId, HttpStatus.BAD_REQUEST);
    }

    Volume volume = (Volume) DataAccessUtils.uniqueResult(hibernateTemplate.findByCriteria(DetachedCriteria
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
  public void update(IssueIdentifier issueId, IssueInputView input) {
    Preconditions.checkNotNull(input);
    Issue issue = getIssue(issueId);
    issue = applyInput(issue, input);
    hibernateTemplate.update(issue);
  }

  @Override
  public VolumeNonAssocView getParentVolumeView(Issue issue) {
    Object[] parentVolumeResults = getParentVolumeByIssue(issue);
    if (parentVolumeResults == null) return null;
    return new VolumeNonAssocView(
        (String) parentVolumeResults[0], (String) parentVolumeResults[1], (String) parentVolumeResults[2],
        (String) parentVolumeResults[3], (String) parentVolumeResults[4]);
  }

  @Override
  public Volume getParentVolume(Issue issue) {
    Object[] parentVolumeResults = getParentVolumeByIssue(issue);
    if (parentVolumeResults == null) return null;
    String volumeUri = (String) parentVolumeResults[0];
    return volumeCrudService.findVolume(DoiBasedIdentity.create(volumeUri));
  }

  private Object[] getParentVolumeByIssue(Issue issue) {
    return hibernateTemplate.execute((Session session) -> {
        String hql = "select volumeUri, displayName, imageUri, title, description " +
            "from Volume where :issue in elements(issues)";
        Query query = session.createQuery(hql);
        query.setParameter("issue", issue);
        return (Object[]) query.uniqueResult();
      });
  }

  @Override
  public void delete(IssueIdentifier issueId) {
    Issue issue = getIssue(issueId);
    if (issue == null) {
      throw new RestClientException("Issue does not exist: " + issueId, HttpStatus.NOT_FOUND);
    }
    List<String> currentIssueJournalKeys = hibernateTemplate.execute((Session session) -> {
      Query query = session.createQuery("select journalKey from Journal where currentIssue = :issue");
      query.setParameter("issue", issue);
      return query.list();
    });
    if (!currentIssueJournalKeys.isEmpty()) {
      String message = String.format("Cannot delete issue: %s. It is the current issue for: %s",
          issueId, currentIssueJournalKeys);
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }

    Volume parentVolume = getParentVolume(issue);
    List<Issue> issues = parentVolume.getIssues();
    issues.remove(issue);
    parentVolume.setIssues(issues);
    hibernateTemplate.save(parentVolume);

    List<IssueArticle> issueArticles = getIssueArticles(issue);
    for (IssueArticle issueArticle : issueArticles) {
      hibernateTemplate.delete(issueArticle);
    }

    hibernateTemplate.delete(issue);
  }

}
