package org.ambraproject.rhino.view.journal;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Issue;
import org.ambraproject.models.Volume;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.KeyedListView;

import java.util.Collection;
import java.util.List;

public class IssueOutputView implements JsonOutputView {

  private final Issue issue;
  private final Optional<Volume> parentVolume;

  public IssueOutputView(Issue issue) {
    this(issue, null);
  }

  public IssueOutputView(Issue issue, Volume parentVolume) {
    this.issue = Preconditions.checkNotNull(issue);
    this.parentVolume = Optional.fromNullable(parentVolume);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(issue).getAsJsonObject();

    serialized.remove("articleDois");
    List<String> articleDois = issue.getArticleDois();
    articleDois = DoiBasedIdentity.asIdentifiers(articleDois);
    serialized.add("articleOrder", context.serialize(articleDois));

    if (parentVolume.isPresent()) {
      VolumeOutputView volumeView = new VolumeOutputView(parentVolume.get(), false);
      serialized.add("parentVolume", context.serialize(volumeView));
    }

    return serialized;
  }

  public static class ListView extends KeyedListView<Issue> {
    private ListView(Collection<? extends Issue> values) {
      super(values);
    }

    @Override
    protected String getKey(Issue value) {
      return DoiBasedIdentity.asIdentifier(value.getIssueUri());
    }

    @Override
    protected Object wrap(Issue value) {
      return new IssueOutputView(value);
    }
  }

  public static KeyedListView<Issue> wrapList(Collection<Issue> issues) {
    return new ListView(issues);
  }

}