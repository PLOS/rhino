package org.ambraproject.rhino.view.journal;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Issue;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.List;

public class IssueOutputView implements JsonOutputView {

  private final Issue issue;
  private final Optional<VolumeNonAssocView> parentVolumeView;

  public IssueOutputView(Issue issue) {
    this(issue, null);
  }

  public IssueOutputView(Issue issue, VolumeNonAssocView parentVolumeView) {
    this.issue = Preconditions.checkNotNull(issue);
    this.parentVolumeView = Optional.fromNullable(parentVolumeView);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(issue).getAsJsonObject();

    serialized.remove("articleDois");
    List<String> articleDois = issue.getArticleDois();
    articleDois = DoiBasedIdentity.asIdentifiers(articleDois);
    serialized.add("articleOrder", context.serialize(articleDois));

    if (parentVolumeView.isPresent()) {
      serialized.add("parentVolume", context.serialize(parentVolumeView.get()));
    }

    return serialized;
  }

}