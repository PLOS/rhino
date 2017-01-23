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

package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.IssueIdentifier;
import org.ambraproject.rhino.identity.VolumeIdentifier;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.CacheableResponse;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.view.journal.IssueInputView;
import org.ambraproject.rhino.view.journal.IssueOutputView;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("JpaQlInspection")
public class IssueCrudServiceImpl extends AmbraService implements IssueCrudService {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private VolumeCrudService volumeCrudService;
  @Autowired
  private IssueOutputView.Factory issueOutputViewFactory;

  @Override
  public Issue readIssue(IssueIdentifier issueId) {
    return getIssue(issueId).orElseThrow(() ->
        new RestClientException("Issue not found with DOI: " + issueId, HttpStatus.NOT_FOUND));
  }

  @Override
  public Optional<Issue> getIssue(IssueIdentifier issueId) {
    return Optional.ofNullable(hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Issue WHERE doi = :doi");
      query.setParameter("doi", issueId.getDoi().getName());
      return (Issue) query.uniqueResult();
    }));
  }

  @Override
  public CacheableResponse<IssueOutputView> serveIssue(final IssueIdentifier id) throws IOException {
    return CacheableResponse.serveEntity(readIssue(id), issueOutputViewFactory::getView);
  }

  private Issue applyInput(Issue issue, IssueInputView input) {
    String issueDoi = input.getDoi();
    if (issueDoi != null) {
      IssueIdentifier issueId = IssueIdentifier.create(issueDoi);
      issue.setDoi(issueId.getDoi().getName());
    }

    String displayName = input.getDisplayName();
    if (displayName != null) {
      issue.setDisplayName(displayName);
    } else if (issue.getDisplayName() == null) {
      issue.setDisplayName("");
    }

    String imageDoi = input.getImageArticleDoi();
    if (imageDoi != null) {
      ArticleIdentifier imageArticleId = ArticleIdentifier.create(imageDoi);
      Article imageArticle = articleCrudService.readArticle(imageArticleId);
      issue.setImageArticle(imageArticle);
    } else {
      issue.setImageArticle(null);
    }

    List<String> inputArticleDois = input.getArticleOrder();
    if (inputArticleDois != null) {
      List<Article> inputArticles = inputArticleDois.stream()
          .map(doi -> articleCrudService.readArticle(ArticleIdentifier.create(doi)))
          .collect(Collectors.toList());
      issue.setArticles(inputArticles);
    }
    return issue;
  }

  @Override
  public Issue create(VolumeIdentifier volumeId, IssueInputView input) {
    Preconditions.checkNotNull(volumeId);

    IssueIdentifier issueId = IssueIdentifier.create(input.getDoi());
    if (getIssue(issueId).isPresent()) {
      throw new RestClientException("Issue already exists with DOI: "
          + issueId, HttpStatus.BAD_REQUEST);
    }
    Issue issue = applyInput(new Issue(), input);
    Volume volume = volumeCrudService.readVolume(volumeId);
    volume.getIssues().add(issue);
    hibernateTemplate.save(volume);
    return issue;
  }

  @Override
  public void update(IssueIdentifier issueId, IssueInputView input) {
    Preconditions.checkNotNull(input);
    Issue issue = readIssue(issueId);
    issue = applyInput(issue, input);
    hibernateTemplate.update(issue);
  }

  @Override
  public Volume getParentVolume(Issue issue) {
    Volume parentVolume = hibernateTemplate.execute((Session session) -> {
      Query query = session.createQuery("from Volume where :issue in elements(issues)");
      query.setParameter("issue", issue);
      return (Volume) query.uniqueResult();
    });
    if (parentVolume == null) {
      throw new RuntimeException("Data integrity error; issue has no parent volume: " + issue.getDoi());
    }
    return parentVolume;
  }

  @Override
  public void delete(IssueIdentifier issueId) {
    Issue issue = readIssue(issueId);

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
    hibernateTemplate.delete(issue);
  }

  @Override
  public Journal getJournalOf(Issue issue) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "SELECT j FROM Journal j, Volume v " +
          "WHERE v IN ELEMENTS(j.volumes) AND :issue IN ELEMENTS(v.issues)");
      query.setParameter("issue", issue);
      return (Journal) query.uniqueResult();
    });
  }
}
