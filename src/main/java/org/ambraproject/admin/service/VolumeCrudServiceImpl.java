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

package org.ambraproject.admin.service;

import com.google.common.base.Preconditions;
import org.ambraproject.admin.RestClientException;
import org.ambraproject.admin.controller.DoiBasedIdentity;
import org.ambraproject.models.Journal;
import org.ambraproject.models.Volume;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

public class VolumeCrudServiceImpl extends AmbraService implements VolumeCrudService {

  @Override
  public void create(DoiBasedIdentity id, String displayName, String journalKey) {
    // TODO Transaction safety
    Journal journal = (Journal) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Journal.class)
            .add(Restrictions.eq("journalKey", journalKey))
            .setFetchMode("volumes", FetchMode.JOIN)
        ));
    if (journal == null) {
      String message = "Journal not found for journal key: " + journalKey;
      HttpStatus status = HttpStatus.BAD_REQUEST; // Not "NOT FOUND", because the URL wasn't wrong
      throw new RestClientException(message, status);
    }

    Volume volume = new Volume();
    volume.setVolumeUri(id.getKey());
    volume.setDisplayName(Preconditions.checkNotNull(displayName));

    List<Volume> volumeList = journal.getVolumes();
    volumeList.add(volume);
    hibernateTemplate.update(journal);
  }

  @Override
  public InputStream readJson(DoiBasedIdentity id) {
    Volume volume = (Volume) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Volume.class)
            .add(Restrictions.eq("volumeUri", id.getKey()))
            .setFetchMode("issues", FetchMode.JOIN)
        ));
    if (volume == null) {
      throw new RestClientException("Volume not found at URI=" + id.getIdentifier(), HttpStatus.NOT_FOUND);
    }
    String volString = entityGson.toJson(volume);
    return new ByteArrayInputStream(volString.getBytes());
  }

}
