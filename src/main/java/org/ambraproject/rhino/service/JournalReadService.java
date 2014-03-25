package org.ambraproject.rhino.service;

import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.util.response.ResponseReceiver;

import java.io.IOException;

public interface JournalReadService {

  public abstract void listJournals(ResponseReceiver receiver, MetadataFormat format) throws IOException;

  public abstract void read(ResponseReceiver receiver, String journalKey, MetadataFormat format) throws IOException;

  /**
   * Writes a list of "in the news" articles to the response.  The implementation will be journal-specific, and some
   * journals may not define this list at all, in which case an exception will be thrown. Each list element corresponds
   * to an article.  The articles' title and doi properties will be present, but other article properties may not be.
   *
   * @param receiver
   * @param journalKey
   * @param format
   * @throws IOException
   */
  public abstract void readInTheNewsArticles(ResponseReceiver receiver, String journalKey, MetadataFormat format)
      throws IOException;
}
