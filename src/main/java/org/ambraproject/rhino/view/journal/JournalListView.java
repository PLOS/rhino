package org.ambraproject.rhino.view.journal;

import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.view.KeyedListView;

import java.util.Collection;

public class JournalListView extends KeyedListView<Journal> {

  public JournalListView(Collection<? extends Journal> values) {
    super(values);
  }

  @Override
  protected String getKey(Journal value) {
    return value.getJournalKey();
  }

}
