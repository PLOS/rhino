package org.ambraproject.rhino.view.journal;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A view of a {@link Journal} object that does not serialize its associative {@code volumes} field. Therefore, it is
 * safe to serialize a Journal wrapped in this class regardless of whether/how its associative fields were initialized.
 * <p/>
 * Other than omitting that field, the view is homomorphic to the default serialization of a bare Journal object.
 */
public class JournalNonAssocView implements JsonOutputView {

  private final Journal journal;

  private JournalNonAssocView(Journal journal) {
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

  /**
   * Return a keyed list view of journals, wrapped such that the journals' associative fields are left alone.
   *
   * @param journals the journals to wrap
   * @return the view
   */
  public static Map<String, JournalNonAssocView> wrapList(Collection<? extends Journal> journals) {
    return journals.stream().collect(Collectors.toMap(Journal::getJournalKey, JournalNonAssocView::new));
  }

}
