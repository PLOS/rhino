package org.ambraproject.rhino.view.journal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.view.JsonOutputView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public class VolumeOutputView implements JsonOutputView {

  public static class Factory {
    @Autowired
    private JournalCrudService journalCrudService;

    public VolumeOutputView getView(Volume volume) {
      return new VolumeOutputView(volume, journalCrudService.readJournalByVolume(volume));
    }
  }

  private final Volume volume;
  private final Journal journal;

  private VolumeOutputView(Volume volume, Journal journal) {
    this.volume = Objects.requireNonNull(volume);
    this.journal = journal;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", volume.getDoi());
    serialized.addProperty("displayName", volume.getDisplayName());
    serialized.addProperty("journalKey", journal.getJournalKey());
    return serialized;
  }

}
