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

import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.response.CacheableResponse;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.view.journal.JournalInputView;
import org.ambraproject.rhino.view.journal.JournalOutputView;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

public interface JournalCrudService {

  public abstract Collection<Journal> getAllJournals();

  public abstract ServiceResponse<Collection<JournalOutputView>> listJournals() throws IOException;

  public abstract CacheableResponse<JournalOutputView> serve(String journalKey) throws IOException;

  public abstract void update(String journalKey, JournalInputView input);

  public abstract Optional<Journal> getJournal(String journalKey);

  public abstract Journal readJournal(String journalKey);

  public abstract Optional<Journal> getJournalByEissn(String eIssn);

  public abstract Journal readJournalByEissn(String eIssn);

  public abstract Journal readJournalByVolume(Volume volume);
}
