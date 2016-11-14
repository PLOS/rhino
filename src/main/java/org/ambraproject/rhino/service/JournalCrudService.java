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
