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
 * A view of a {@link Journal} object that shows all nested volume and issue lists as JSON objects keyed by their
 * identifying URIs. Compare {@link JournalNonAssocView}, which omits the nested lists entirely.
 */
public class JournalOutputView implements JsonOutputView {

  public static class Factory {
    @Autowired
    private IssueOutputView.Factory issueOutputViewFactory;
    @Autowired
    private VolumeOutputView.Factory volumeOutputViewFactory;

    public JournalOutputView getView(Journal journal) {
      return new JournalOutputView(issueOutputViewFactory, volumeOutputViewFactory, journal, true);
    }

    public JournalOutputView getShallowView(Journal journal) {
      return new JournalOutputView(issueOutputViewFactory, volumeOutputViewFactory, journal, false);
    }
  }

  private final IssueOutputView.Factory issueOutputViewFactory;
  private final VolumeOutputView.Factory volumeOutputViewFactory;

  private final Journal journal;
  private final boolean showChildren;

  private JournalOutputView(IssueOutputView.Factory issueOutputViewFactory,
                            VolumeOutputView.Factory volumeOutputViewFactory,
                            Journal journal, boolean showChildren) {
    this.journal = Objects.requireNonNull(journal);
    this.issueOutputViewFactory = Objects.requireNonNull(issueOutputViewFactory);
    this.volumeOutputViewFactory = Objects.requireNonNull(volumeOutputViewFactory);
    this.showChildren = showChildren;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("journalKey", journal.getJournalKey());
    serialized.addProperty("title", journal.getTitle());
    serialized.addProperty("eIssn", journal.geteIssn());

    if (showChildren) {
      Issue currentIssue = journal.getCurrentIssue();
      if (currentIssue != null) {
        IssueOutputView currentIssueView = issueOutputViewFactory.getView(currentIssue);
        serialized.add("currentIssue", currentIssueView.serialize(context));
      }

      List<VolumeOutputView> volumeOutputViews = new ArrayList<>();
      for (Volume volume : journal.getVolumes()) {
        VolumeOutputView volumeOutputView = volumeOutputViewFactory.getView(volume);
        volumeOutputViews.add(volumeOutputView);
      }
      serialized.add("volumes", context.serialize(volumeOutputViews));
    }

    return serialized;
  }

}
