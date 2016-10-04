package org.ambraproject.rhino.view.journal;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.List;
import java.util.Objects;

public class VolumeOutputView implements JsonOutputView {

  public static VolumeOutputView getView(Volume volume) {
    return new VolumeOutputView(volume);
  }

  private final Volume volume;

  private VolumeOutputView(Volume volume) {
    this.volume = Objects.requireNonNull(volume);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", volume.getDoi());
    serialized.addProperty("displayName", volume.getDisplayName());

    List<IssueOutputView> issueViews = Lists.transform(volume.getIssues(), IssueOutputView::getShallowView);
    serialized.add("issues", context.serialize(issueViews));

    return serialized;
  }

}
