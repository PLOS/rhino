package org.ambraproject.rhino.view.journal;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.List;
import java.util.stream.Collectors;

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

    List<String> articleDois = issue.getArticles().stream()
        .map(ArticleTable::getDoi).collect(Collectors.toList());
    serialized.add("articleOrder", context.serialize(articleDois));

    if (parentVolumeView.isPresent()) {
      serialized.add("parentVolume", context.serialize(parentVolumeView.get()));
    }

    return serialized;
  }

}