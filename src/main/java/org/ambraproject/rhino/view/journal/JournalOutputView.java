package org.ambraproject.rhino.view.journal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.view.JsonOutputView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A view of a {@link Journal} object.
 */
public class JournalOutputView implements JsonOutputView {

  public static class Factory {
    @Autowired
    private IssueOutputView.Factory issueOutputViewFactory;

    public JournalOutputView getView(Journal journal) {
      return new DeepView(journal, issueOutputViewFactory);
    }
  }

  public static JournalOutputView getShallowView(Journal journal) {
    return new JournalOutputView(journal);
  }

  protected final Journal journal;

  private JournalOutputView(Journal journal) {
    this.journal = Objects.requireNonNull(journal);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("journalKey", journal.getJournalKey());
    serialized.addProperty("title", journal.getTitle());
    serialized.addProperty("eIssn", journal.geteIssn());

    addChildren(context, serialized);

    return serialized;
  }

  protected void addChildren(JsonSerializationContext context, JsonObject serialized) {
  }

  /**
   * Shows all nested volume and issue lists as JSON objects keyed by their identifying URIs.
   */
  public static class DeepView extends JournalOutputView {
    private final IssueOutputView.Factory issueOutputViewFactory;

    private DeepView(Journal journal, IssueOutputView.Factory issueOutputViewFactory) {
      super(journal);
      this.issueOutputViewFactory = Objects.requireNonNull(issueOutputViewFactory);
    }

    @Override
    protected void addChildren(JsonSerializationContext context, JsonObject serialized) {
      Issue currentIssue = journal.getCurrentIssue();
      if (currentIssue != null) {
        IssueOutputView currentIssueView = issueOutputViewFactory.getView(currentIssue);
        serialized.add("currentIssue", currentIssueView.serialize(context));
      }

      List<VolumeOutputView> volumeOutputViews = new ArrayList<>();
      for (Volume volume : journal.getVolumes()) {
        VolumeOutputView volumeOutputView = VolumeOutputView.getView(volume);
        volumeOutputViews.add(volumeOutputView);
      }
      serialized.add("volumes", context.serialize(volumeOutputViews));
    }
  }

}
