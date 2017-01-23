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

import org.ambraproject.rhino.identity.IssueIdentifier;
import org.ambraproject.rhino.identity.VolumeIdentifier;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.CacheableResponse;
import org.ambraproject.rhino.view.journal.IssueInputView;
import org.ambraproject.rhino.view.journal.IssueOutputView;

import java.io.IOException;
import java.util.Optional;

public interface IssueCrudService {

  /**
   * Serve issue metadata to a client.
   */
  public abstract CacheableResponse<IssueOutputView> serveIssue(IssueIdentifier id) throws IOException;

  /**
   * Read a issue requested by the client, throwing {@link RestClientException} if the issue does not exist.
   */
  public abstract Issue readIssue(IssueIdentifier issueId);

  /**
   * Get a issue if it exists.
   */
  public abstract Optional<Issue> getIssue(IssueIdentifier issueId);

  public abstract Issue create(VolumeIdentifier volumeId, IssueInputView input);

  public abstract void update(IssueIdentifier issueId, IssueInputView input);

  public abstract Volume getParentVolume(Issue issue);

  public abstract void delete(IssueIdentifier issueId);

  public abstract Journal getJournalOf(Issue issue);
}
