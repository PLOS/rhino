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

package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.VolumeIdentifier;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.CacheableResponse;
import org.ambraproject.rhino.view.journal.VolumeInputView;
import org.ambraproject.rhino.view.journal.VolumeOutputView;

import java.io.IOException;
import java.util.Optional;

public interface VolumeCrudService {

  /**
   * Serve volume metadata to a client.
   */
  public abstract CacheableResponse<VolumeOutputView> serveVolume(VolumeIdentifier id) throws IOException;

  /**
   * Read a volume requested by the client, throwing {@link RestClientException} if the volume does not exist.
   */
  public abstract Volume readVolume(VolumeIdentifier volumeId);

  /**
   * Get a volume if it exists.
   */
  public abstract Optional<Volume> getVolume(VolumeIdentifier volumeId);

  public abstract Volume readVolumeByIssue(Issue issueId);

  public abstract Volume create(String journalKey, VolumeInputView input);

  public abstract Volume update(VolumeIdentifier volumeId, VolumeInputView input);

  public abstract void delete(VolumeIdentifier id) throws IOException;

  public abstract Journal getJournalOf(Volume volume);
}
