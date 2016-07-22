package org.ambraproject.rhino.service;

import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.journal.JournalInputView;

import java.io.IOException;
import java.util.Optional;

public interface JournalCrudService {

  public abstract Transceiver listJournals() throws IOException;

  public abstract Transceiver read(String journalKey) throws IOException;

  public abstract void update(String journalKey, JournalInputView input);

  public abstract Transceiver readCurrentIssue(String journalKey);

  public abstract Optional<Journal> getJournal(String journalKey);

  public abstract Journal findJournal(String journalKey);

  public abstract Optional<Journal> getJournalByEissn(String eIssn);

  public abstract Journal findJournalByEissn(String eIssn);
}
