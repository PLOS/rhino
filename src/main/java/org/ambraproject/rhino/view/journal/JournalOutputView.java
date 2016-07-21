package org.ambraproject.rhino.view.journal;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.view.JsonOutputView;

/**
 * A view of a {@link Journal} object that shows all nested volume and issue lists as JSON objects keyed by their
 * identifying URIs. Compare {@link JournalNonAssocView}, which omits the nested lists entirely.
 */
public class JournalOutputView implements JsonOutputView {

  private final Journal journal;

  public JournalOutputView(Journal journal) {
    this.journal = Preconditions.checkNotNull(journal);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();

    //todo: fix the stack overflow caused by the following line
    //JsonObject serialized = context.serialize(journal).getAsJsonObject();

    serialized.add("journalKey", context.serialize(journal.getJournalKey()));
    serialized.add("eIssn", context.serialize(journal.getJournalKey()));
    serialized.add("title", context.serialize(journal.getTitle()));
    serialized.add("created", context.serialize(journal.getCreated()));
    serialized.add("lastModified", context.serialize(journal.getLastModified()));

    if (journal.getImageArticle() != null) {
      serialized.add("imageArticleDoi", context.serialize(journal.getImageArticle().getDoi()));
    }

    if (journal.getCurrentIssue() != null) {
      serialized.add("currentIssueDoi", context.serialize(journal.getCurrentIssue().getDoi()));
    }

    return serialized;
  }

}
