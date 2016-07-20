/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.VolumeIdentifier;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.journal.VolumeInputView;

import java.io.IOException;

public interface VolumeCrudService {

  public abstract Volume getVolume(VolumeIdentifier volumeId);

  public abstract VolumeIdentifier create(String journalKey, VolumeInputView input);

  public abstract void update(VolumeIdentifier volumeId, VolumeInputView input);

  public abstract Transceiver read(VolumeIdentifier id) throws IOException;

  public abstract void delete(VolumeIdentifier id) throws IOException;

}
