package org.ambraproject.rhino.view.journal;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.view.JsonOutputView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

public class VolumeOutputView implements JsonOutputView {

  public static class Factory {
    @Autowired private IssueOutputView.Factory issueOutputViewFactory;

    public VolumeOutputView getView(Volume volume) {
      return new VolumeOutputView(this, volume);
    }
  }

  private final Volume volume;
  private final VolumeOutputView.Factory factory;

  private VolumeOutputView(Factory factory, Volume volume) {
    this.volume = Objects.requireNonNull(volume);
    this.factory = Objects.requireNonNull(factory);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(volume).getAsJsonObject();
    List<IssueOutputView> issueViews = Lists.transform(volume.getIssues(),
        issue -> factory.issueOutputViewFactory.getView(issue, volume));
    serialized.add("issues", context.serialize(issueViews));
    return serialized;
  }

}
