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
import org.ambraproject.rhino.identity.VolumeIdentifier;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.CacheableResponse;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
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
  public CacheableResponse<VolumeOutputView> serveVolume(final VolumeIdentifier id) throws IOException {
    return CacheableResponse.serveEntity(readVolume(id), volumeOutputViewFactory::getView);
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

    return volume;
  }

  @Override
  public Journal getJournalOf(Volume volume) {
    return hibernateTemplate.execute(session->{
      Query query = session.createQuery("FROM Journal WHERE :volume IN ELEMENTS(volumes)");
      query.setParameter("volume", volume);
      return (Journal) query.uniqueResult();
    });
  }
}
