package org.ambraproject.rhino.view.journal;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Volume;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.List;

public class VolumeOutputView implements JsonOutputView {

  private final Volume volume;

  public VolumeOutputView(Volume volume) {
    this.volume = Preconditions.checkNotNull(volume);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(volume).getAsJsonObject();
    List<IssueOutputView> issueViews = Lists.transform(volume.getIssues(), IssueOutputView::new);
    serialized.add("issues", context.serialize(issueViews));
    return serialized;
  }

}
