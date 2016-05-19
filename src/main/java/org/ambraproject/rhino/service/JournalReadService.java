package org.ambraproject.rhino.service;

import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.util.response.Transceiver;

import java.io.IOException;

public interface JournalReadService {

  public abstract Transceiver listJournals() throws IOException;

  public abstract Transceiver read(String journalKey) throws IOException;

  public abstract Transceiver readCurrentIssue(String journalKey);

  Journal getJournal(String journalKey);
}
