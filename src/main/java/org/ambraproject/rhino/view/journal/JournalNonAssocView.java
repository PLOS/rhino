package org.ambraproject.rhino.view.journal;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Journal;
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

  private JournalNonAssocView(Journal journal) {
    this.journal = Preconditions.checkNotNull(journal);
  }

  private static final Function<Journal, JournalNonAssocView> WRAP = new Function<Journal, JournalNonAssocView>() {
    @Override
    public JournalNonAssocView apply(Journal input) {
      return new JournalNonAssocView(input);
    }
  };

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("journalKey", journal.getJournalKey());
    serialized.addProperty("eIssn", journal.geteIssn());
    serialized.addProperty("imageUri", journal.getImageUri());
    serialized.addProperty("title", journal.getTitle());
    serialized.addProperty("description", journal.getDescription());
    return serialized;
  }

  public static class ListView extends KeyedListView<JournalNonAssocView> {
    private ListView(Collection<? extends JournalNonAssocView> values) {
      super(values);
    }

    @Override
    protected String getKey(JournalNonAssocView value) {
      return value.journal.getJournalKey();
    }
  }

  /**
   * Return a keyed list view of journals, wrapped such that the journals' associative fields are left alone. The
   * returned list view is homomorphic to {@link JournalListView}.
   *
   * @param journals the journals to wrap
   * @return the view
   */
  public static KeyedListView<JournalNonAssocView> wrapList(Collection<Journal> journals) {
    Collection<JournalNonAssocView> viewList = Collections2.transform(journals, WRAP);
    return new ListView(viewList);
  }

}
