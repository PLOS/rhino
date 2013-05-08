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
import org.ambraproject.models.Journal;
import org.ambraproject.models.Volume;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.view.journal.VolumeInputView;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.List;

public class VolumeCrudServiceImpl extends AmbraService implements VolumeCrudService {

  private static Volume applyInput(Volume volume, VolumeInputView input) {
    String volumeUri = input.getVolumeUri();
    if (volumeUri != null) {
      DoiBasedIdentity volumeId = DoiBasedIdentity.create(volumeUri);
      volume.setVolumeUri(volumeId.getKey());
    }

    String displayName = input.getDisplayName();
    if (displayName != null) {
      volume.setDisplayName(displayName);
    } else if (volume.getDisplayName() == null) {
      volume.setDisplayName("");
    }

    String imageUri = input.getImageUri();
    if (imageUri != null) {
      ArticleIdentity imageArticleId = ArticleIdentity.create(imageUri);
      volume.setImageUri(imageArticleId.getKey());
    } else if (volume.getImageUri() == null) {
      volume.setImageUri("");
    }

    return volume;
  }

  @Override
  public DoiBasedIdentity create(String journalKey, VolumeInputView input) {
    Preconditions.checkNotNull(journalKey);
    Preconditions.checkNotNull(input.getVolumeUri());

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

    Volume volume = applyInput(new Volume(), input);

    List<Volume> volumeList = journal.getVolumes();
    volumeList.add(volume);
    hibernateTemplate.update(journal);

    return DoiBasedIdentity.create(volume.getVolumeUri());
  }

  @Override
  public void update(DoiBasedIdentity volumeId, VolumeInputView input) {
    Preconditions.checkNotNull(input);
    Volume volume = (Volume) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Volume.class)
            .add(Restrictions.eq("volumeUri", volumeId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    volume = applyInput(volume, input);
    hibernateTemplate.update(volume);
  }

  @Override
  public void read(ResponseReceiver receiver, DoiBasedIdentity id, MetadataFormat mf) throws IOException {
    assert mf == MetadataFormat.JSON;
    Volume volume = (Volume) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Volume.class)
            .add(Restrictions.eq("volumeUri", id.getKey()))
            .setFetchMode("issues", FetchMode.JOIN)
        ));
    if (volume == null) {
      throw new RestClientException("Volume not found at URI=" + id.getIdentifier(), HttpStatus.NOT_FOUND);
    }
    writeJson(receiver, volume);
  }

}
