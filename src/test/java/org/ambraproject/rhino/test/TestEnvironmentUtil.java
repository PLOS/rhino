package org.ambraproject.rhino.test;

import com.google.common.base.Preconditions;
import org.ambraproject.models.Journal;

public class TestEnvironmentUtil {
  private TestEnvironmentUtil() {
    throw new AssertionError("Not instantiable");
  }

  /**
   * Create a dummy journal with required non-null fields filled in.
   *
   * @param eissn the dummy journal's eIssn
   * @return a new dummy journal object
   */
  public static Journal createDummyJournal(String eissn) {
    Preconditions.checkNotNull(eissn);
    Journal journal = new Journal();
    String title = "Test Journal " + eissn;
    journal.setTitle(title);
    journal.setJournalKey(title.replaceAll("\\s|-", ""));
    journal.seteIssn(eissn);
    return journal;
  }

}
