package org.ambraproject.rhino.service;

import org.ambraproject.rhino.util.response.Transceiver;

import java.io.IOException;

public interface JournalReadService {

  public abstract Transceiver listJournals() throws IOException;

  public abstract Transceiver read(String journalKey) throws IOException;

  /**
   * Writes a list of "in the news" articles to the response.  The implementation will be journal-specific, and some
   * journals may not define this list at all, in which case an exception will be thrown. Each list element corresponds
   * to an article.  The articles' title and doi properties will be present, but other article properties may not be.
   *
   * @param journalKey
   * @throws IOException
   */
  public abstract Transceiver readInTheNewsArticles(String journalKey)
      throws IOException;
}
