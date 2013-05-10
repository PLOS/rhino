package org.ambraproject.rhino.view.journal;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Volume;
import org.ambraproject.rhino.identity.ArticleIdentity;
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
    KeyedListView<IssueOutputView> issueView = IssueOutputView.wrapList(volume.getIssues());
    serialized.add("issues", context.serialize(issueView));
    return serialized;
  }

  private static final Function<Volume, VolumeOutputView> WRAP = new Function<Volume, VolumeOutputView>() {
    @Override
    public VolumeOutputView apply(Volume input) {
      return new VolumeOutputView(input);
    }
  };

  public static class ListView extends KeyedListView<VolumeOutputView> {
    private ListView(Collection<? extends VolumeOutputView> values) {
      super(values);
    }

    @Override
    protected String getKey(VolumeOutputView value) {
      return ArticleIdentity.removeScheme(value.volume.getVolumeUri());
    }
  }

  public static KeyedListView<VolumeOutputView> wrapList(Collection<Volume> volumes) {
    Collection<VolumeOutputView> viewList = Collections2.transform(volumes, WRAP);
    return new ListView(viewList);
  }

}
