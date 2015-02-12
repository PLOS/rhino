package org.ambraproject.rhino.service.taxonomy;

import org.ambraproject.rhino.util.response.Transceiver;

import java.io.IOException;

/**
 * Service for looking up taxonomic terms that have been previously assigned to an article and persisted..
 */
public interface TaxonomyLookupService {

  /**
   * Forwards the child terms of a given taxonomic subject area to the receiver, along with the counts of children for
   * each child.
   *
   * @param journal journal key specifying the journal
   * @param parent  the parent subject category that we will return children for.  If null or empty, the root of the
   *                hierarchy will be used.
   * @throws java.io.IOException
   */
  Transceiver read(String journal, String parent) throws IOException;

}
