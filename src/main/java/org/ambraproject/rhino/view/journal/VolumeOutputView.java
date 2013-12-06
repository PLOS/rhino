package org.ambraproject.rhino.view.journal;

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

public class VolumeOutputView implements JsonOutputView {

  private final Volume volume;

  public VolumeOutputView(Volume volume) {
    this.volume = Preconditions.checkNotNull(volume);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(volume).getAsJsonObject();
    KeyedListView<Issue> issueView = IssueOutputView.wrapList(volume.getIssues());
    serialized.add("issues", context.serialize(issueView));
    return serialized;
  }

  public static class ListView extends KeyedListView<Volume> {
    private ListView(Collection<? extends Volume> values) {
      super(values);
    }

    @Override
    protected String getKey(Volume value) {
      return DoiBasedIdentity.asIdentifier(value.getVolumeUri());
    }

    @Override
    protected Object wrap(Volume value) {
      return new VolumeOutputView(value);
    }
  }

  public static KeyedListView<Volume> wrapList(Collection<Volume> volumes) {
    return new ListView(volumes);
  }

}
