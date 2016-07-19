package org.ambraproject.rhino.view.journal;

import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.view.KeyedListView;

import java.util.Collection;

public class VolumeListView extends KeyedListView<Volume> {

  public VolumeListView(Collection<? extends Volume> values) {
    super(values);
  }

  @Override
  protected String getKey(Volume value) {
    return DoiBasedIdentity.asIdentifier(value.getDoi());
  }

}
