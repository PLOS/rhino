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
import org.ambraproject.rhino.model.Issue;
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
import java.util.ArrayList;
import java.util.Optional;

@SuppressWarnings("JpaQlInspection")
public class VolumeCrudServiceImpl extends AmbraService implements VolumeCrudService {

  @Autowired
  private JournalCrudService journalCrudService;
  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private VolumeOutputView.Factory volumeOutputViewFactory;

  @Override
  public Volume readVolume(VolumeIdentifier volumeId) {
    return getVolume(volumeId).orElseThrow(() ->
        new RestClientException("Volume not found with DOI: " + volumeId.getDoi(), HttpStatus.NOT_FOUND));
  }

  @Override
  public Optional<Volume> getVolume(VolumeIdentifier volumeId) {
    return Optional.ofNullable(hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Volume WHERE doi = :doi");
      query.setParameter("doi", volumeId.getDoi().getName());
      return (Volume) query.uniqueResult();
    }));
  }

  @Override
  public Volume readVolumeByIssue(Issue issue) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Volume WHERE :issue IN ELEMENTS(issues)");
      query.setParameter("issue", issue);
      return (Volume) query.uniqueResult();
    });
  }

  @Override
  public Volume create(String journalKey, VolumeInputView input) {
    Preconditions.checkNotNull(journalKey);

    VolumeIdentifier volumeId = VolumeIdentifier.create(input.getDoi());
    if (getVolume(volumeId).isPresent()) {
      throw new RestClientException("Volume already exists with DOI: "
          + volumeId.getDoi(), HttpStatus.BAD_REQUEST);
    }

    Volume volume = new Volume();
    volume.setIssues(new ArrayList<>(0));
    volume = applyInput(volume, input);
    Journal journal = journalCrudService.readJournal(journalKey);
    journal.getVolumes().add(volume);
    hibernateTemplate.save(journal);

    return volume;
  }

  @Override
  public Volume update(VolumeIdentifier volumeId, VolumeInputView input) {
    Preconditions.checkNotNull(input);
    Volume volume = readVolume(volumeId);
    volume = applyInput(volume, input);
    hibernateTemplate.update(volume);
    return volume;
  }

  @Override
  public Transceiver serveVolume(final VolumeIdentifier id) throws IOException {
    return new EntityTransceiver<Volume>() {
      @Override
      protected Volume fetchEntity() {
        return readVolume(id);
      }

      @Override
      protected Object getView(Volume volume) {
        return volumeOutputViewFactory.getView(volume);
      }
    };
  }

  @Override
  public void delete(VolumeIdentifier id) throws IOException {
    Volume volume = readVolume(id);
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
      ArticleTable imageArticle = articleCrudService.readArticle(imageArticleId);
      volume.setImageArticle(imageArticle);
    } else {
      volume.setImageArticle(null);
    }
    return volume;
  }
}
