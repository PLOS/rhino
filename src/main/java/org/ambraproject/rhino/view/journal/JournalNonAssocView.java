package org.ambraproject.rhino.view.journal;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.KeyedListView;

import java.util.Collection;

/**
 * A view of a {@link Journal} object that does not serialize its associative {@code volumes} field. Therefore, it is
 * safe to serialize a Journal wrapped in this class regardless of whether/how its associative fields were initialized.
 * <p/>
 * Other than omitting that field, the view is homomorphic to the default serialization of a bare Journal object.
 */
public class JournalNonAssocView implements JsonOutputView {

  private final Journal journal;

  public JournalNonAssocView(Journal journal) {
    this.journal = Preconditions.checkNotNull(journal);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("journalKey", journal.getJournalKey());
    serialized.addProperty("eIssn", journal.geteIssn());
    serialized.addProperty("title", journal.getTitle());
    return serialized;
  }

  public static class ListView extends KeyedListView<Journal> {
    private ListView(Collection<? extends Journal> values) {
      super(values);
    }

    @Override
    protected String getKey(Journal value) {
      return value.getJournalKey();
    }

    @Override
    protected Object wrap(Journal value) {
      return new JournalNonAssocView(value);
    }
  }

  /**
   * Return a keyed list view of journals, wrapped such that the journals' associative fields are left alone. The
   * returned list view is homomorphic to {@link JournalListView}.
   *
   * @param journals the journals to wrap
   * @return the view
   */
  public static ListView wrapList(Collection<? extends Journal> journals) {
    return new ListView(journals);
  }

}
