package org.ambraproject.rhino.view.journal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.view.JsonOutputView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.Optional;

/**
 * A view of a {@link Journal} object.
 */
public class JournalOutputView implements JsonOutputView {

  public static JournalOutputView getView(Journal journal) {
    return new JournalOutputView(journal);
  }

  private final Journal journal;

  private JournalOutputView(Journal journal) {
    this.journal = Objects.requireNonNull(journal);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("journalKey", journal.getJournalKey());
    serialized.addProperty("title", journal.getTitle());
    serialized.addProperty("eIssn", journal.geteIssn());

    return serialized;
  }

}
