package org.ambraproject.rhino.view.journal;

import org.ambraproject.models.Volume;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.view.KeyedListView;

import java.util.Collection;

public class VolumeListView extends KeyedListView<Volume> {

  public VolumeListView(Collection<? extends Volume> values) {
    super(values);
  }

  @Override
  protected String getKey(Volume value) {
    return ArticleIdentity.removeScheme(value.getVolumeUri());
  }

}
