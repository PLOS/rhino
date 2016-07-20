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
import org.ambraproject.rhino.identity.VolumeIdentifier;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.journal.VolumeInputView;
import org.ambraproject.rhino.view.journal.VolumeOutputView;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;

@SuppressWarnings("JpaQlInspection")
public class VolumeCrudServiceImpl extends AmbraService implements VolumeCrudService {

  @Autowired
  private JournalCrudService journalCrudService;

  @Autowired
  private ArticleCrudService articleCrudService;

  @Override
  public Volume getVolume(VolumeIdentifier volumeId) {
    Volume volume = getVolumeFromDatabase(volumeId);
    if (volume == null) {
      throw new RestClientException("Volume not found with DOI: " + volumeId.getDoi(),
          HttpStatus.NOT_FOUND);
    }
    return volume;
  }

  private Volume getVolumeFromDatabase(VolumeIdentifier volumeId) {
    return hibernateTemplate.execute(session -> {
        Query query = session.createQuery("FROM Volume WHERE doi = :doi");
        query.setParameter("doi", volumeId.getDoi().getName());
        return (Volume) query.uniqueResult();
      });
  }

  @Override
  public VolumeIdentifier create(String journalKey, VolumeInputView input) {
    Preconditions.checkNotNull(journalKey);

    VolumeIdentifier volumeId = VolumeIdentifier.create(input.getDoi());
    if (getVolumeFromDatabase(volumeId) != null) {
      throw new RestClientException("Volume already exists with DOI: "
          + volumeId.getDoi(), HttpStatus.BAD_REQUEST);
    }

    Volume volume = applyInput(new Volume(), input);
    Journal journal = journalCrudService.findJournal(journalKey);
    volume.setJournal(journal);

    hibernateTemplate.save(volume);

    return VolumeIdentifier.create(volume.getDoi());
  }

  @Override
  public void update(VolumeIdentifier volumeId, VolumeInputView input) {
    Preconditions.checkNotNull(input);
    Volume volume = getVolume(volumeId);
    volume = applyInput(volume, input);
    hibernateTemplate.update(volume);
  }

  @Override
  public Transceiver read(final VolumeIdentifier id) throws IOException {
    return new EntityTransceiver<Volume>() {
      @Override
      protected Volume fetchEntity() {
        return getVolume(id);
      }

      @Override
      protected Object getView(Volume volume) {
        return new VolumeOutputView(volume);
      }
    };
  }

  @Override
  public void delete(VolumeIdentifier id) throws IOException {
    Volume volume = getVolume(id);
    if (volume.getIssues().size() > 0) {
      throw new RestClientException("Volume has issues and cannot be deleted until all issues have " +
          "been deleted.", HttpStatus.BAD_REQUEST);
    }
    hibernateTemplate.delete(volume);
  }

  private Volume applyInput(Volume volume, VolumeInputView input) {
    String doi = input.getDoi();
    if (doi != null) {
      VolumeIdentifier volumeId = VolumeIdentifier.create(doi);
      volume.setDoi(volumeId.getDoi().getName());
    }

    String displayName = input.getDisplayName();
    if (displayName != null) {
      volume.setDisplayName(displayName);
    } else if (volume.getDisplayName() == null) {
      volume.setDisplayName("");
    }

    String imageUri = input.getImageArticleDoi();
    if (imageUri != null) {
      ArticleIdentifier imageArticleId = ArticleIdentifier.create(imageUri);
      ArticleTable imageArticle = articleCrudService.getArticle(imageArticleId);
      volume.setImageArticle(imageArticle);
    } else if (volume.getImageArticle() == null) {
      volume.setImageArticle(null);
    }
    return volume;
  }
}
