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

import com.google.common.base.Optional;
import org.ambraproject.models.Issue;
import org.ambraproject.models.Volume;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

public class IssueCrudServiceImpl extends AmbraService implements IssueCrudService {

  @Override
  public void read(ResponseReceiver receiver, DoiBasedIdentity id, MetadataFormat mf) throws IOException {
    assert mf == MetadataFormat.JSON;
    Issue issue = (Issue) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Issue.class)
            .add(Restrictions.eq("issueUri", id.getIdentifier())) // not getKey
        ));
    if (issue == null) {
      throw new RestClientException("Issue not found at URI=" + id.getIdentifier(), HttpStatus.NOT_FOUND);
    }
    writeJson(receiver, issue);
  }

  @Override
  public void create(DoiBasedIdentity volumeId, DoiBasedIdentity issueId,
                     Optional<String> displayName, Optional<ArticleIdentity> imageArticleId) {
    Issue issue = new Issue();
    issue.setIssueUri(issueId.getIdentifier()); // not getKey
    issue.setDisplayName(displayName.or(""));
    issue.setImageUri(imageArticleId.isPresent() ? imageArticleId.get().getKey() : "");

    Volume volume = (Volume) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Volume.class)
            .add(Restrictions.eq("volumeUri", volumeId.getKey()))
            .setFetchMode("issues", FetchMode.JOIN)
        ));
    if (volume == null) {
      throw new RestClientException("Volume not found for volumeUri: " + volumeId.getIdentifier(), HttpStatus.BAD_REQUEST);
    }
    List<Issue> volumeIssues = volume.getIssues();
    volumeIssues.add(issue);
    hibernateTemplate.update(volume);
  }

}
