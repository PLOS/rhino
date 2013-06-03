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
import org.ambraproject.models.Issue;
import org.ambraproject.models.Volume;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.view.journal.IssueInputView;
import org.ambraproject.rhino.view.journal.IssueOutputView;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IssueCrudServiceImpl extends AmbraService implements IssueCrudService {

  @Override
  public void read(ResponseReceiver receiver, DoiBasedIdentity id, MetadataFormat mf) throws IOException {
    assert mf == MetadataFormat.JSON;
    Issue issue = (Issue) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Issue.class)
            .add(Restrictions.eq("issueUri", id.getKey()))
        ));
    if (issue == null) {
      throw new RestClientException("Issue not found at URI=" + id.getIdentifier(), HttpStatus.NOT_FOUND);
    }
    writeJson(receiver, new IssueOutputView(issue));
  }

  private static Issue applyInput(Issue issue, IssueInputView input) {
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
      issue.setImageUri(imageArticleId.getKey());
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

  @Override
  public DoiBasedIdentity create(DoiBasedIdentity volumeId, IssueInputView input) {
    Preconditions.checkNotNull(volumeId);
    Preconditions.checkNotNull(input.getIssueUri());

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
    Issue issue = (Issue) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Issue.class)
            .add(Restrictions.eq("issueUri", issueId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (issue == null) {
      throw new RestClientException("Issue not found for issueUri: " + issueId.getIdentifier(), HttpStatus.BAD_REQUEST);
    }
    issue = applyInput(issue, input);
    hibernateTemplate.update(issue);
  }

}
