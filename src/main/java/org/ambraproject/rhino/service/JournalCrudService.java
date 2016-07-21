package org.ambraproject.rhino.service;

import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.journal.JournalInputView;

import java.io.IOException;

public interface JournalCrudService {

  public abstract Transceiver listJournals() throws IOException;

  public abstract Transceiver serve(String journalKey) throws IOException;

  public abstract void update(String journalKey, JournalInputView input);

  public abstract Transceiver serveCurrentIssue(String journalKey);

  public abstract Journal readJournal(String journalKey);

  public abstract Journal readJournalByEissn(String eIssn);
}
