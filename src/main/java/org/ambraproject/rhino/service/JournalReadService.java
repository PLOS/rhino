package org.ambraproject.rhino.service;

import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.util.response.ResponseReceiver;

import java.io.IOException;

public interface JournalReadService {

  public abstract void read(ResponseReceiver receiver, MetadataFormat format) throws IOException;

}
