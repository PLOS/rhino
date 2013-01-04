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

package org.ambraproject.soa.service.impl;

import org.ambraproject.models.Issue;
import org.ambraproject.models.Volume;
import org.ambraproject.soa.identity.DoiBasedIdentity;
import org.ambraproject.soa.rest.RestClientException;
import org.ambraproject.soa.service.IssueCrudService;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import java.util.List;

public class IssueCrudServiceImpl extends AmbraService implements IssueCrudService {

  @Override
  public void create(String volumeUri, DoiBasedIdentity issueId, String issueDisplayName, String issueImageUri) {
    Issue issue = new Issue();
    issue.setIssueUri(issueId.getKey());
    issue.setDisplayName(issueDisplayName);
    issue.setImageUri(issueImageUri);

    Volume volume = (Volume) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Volume.class)
            .add(Restrictions.eq("volumeUri", volumeUri))
            .setFetchMode("issues", FetchMode.JOIN)
        ));
    if (volume == null) {
      throw new RestClientException("Volume not found for volumeUri: " + volumeUri, HttpStatus.BAD_REQUEST);
    }
    List<Issue> volumeIssues = volume.getIssues();
    volumeIssues.add(issue);
    hibernateTemplate.update(volume);
  }

}
