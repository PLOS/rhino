package org.ambraproject.rhino.service;

import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.journal.JournalInputView;

import java.io.IOException;

public interface JournalCrudService {

  public abstract Transceiver listJournals() throws IOException;

  public abstract Transceiver read(String journalKey) throws IOException;

  public abstract void update(String journalKey, JournalInputView input);

  public abstract Transceiver readCurrentIssue(String journalKey);

  Journal getJournal(String journalKey);
}
