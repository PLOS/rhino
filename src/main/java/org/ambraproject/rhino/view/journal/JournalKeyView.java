package org.ambraproject.rhino.view.journal;

import org.ambraproject.rhino.view.KeyedSingletonListView;

import java.util.Collection;

public class JournalKeyView extends KeyedSingletonListView<String> {

  public JournalKeyView(Collection<? extends String> journalKeys) {
    super("journalKey", journalKeys);
  }

  @Override
  protected String getKey(String value) {
    return value;
  }

}
