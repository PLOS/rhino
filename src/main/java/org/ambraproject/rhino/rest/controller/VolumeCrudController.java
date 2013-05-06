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

package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class VolumeCrudController extends DoiBasedCrudController {

  private static final String VOLUME_ROOT = "/volumes";
  private static final String VOLUME_NAMESPACE = VOLUME_ROOT + '/';
  private static final String VOLUME_TEMPLATE = VOLUME_NAMESPACE + "**";

  private static final String ID_PARAM = "id";
  private static final String DISPLAY_PARAM = "display";
  private static final String JOURNAL_PARAM = "journal";

  @Override
  protected String getNamespacePrefix() {
    return VOLUME_NAMESPACE;
  }

  @Autowired
  private VolumeCrudService volumeCrudService;


  private static void validateNonEmpty(String name, String value) {
    if (value.isEmpty()) {
      String message = "Non-blank value required for parameter: " + name;
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
  }

  @RequestMapping(value = VOLUME_ROOT, method = RequestMethod.POST)
  public ResponseEntity<?> create(@RequestParam(ID_PARAM) String volumeId,
                                  @RequestParam(DISPLAY_PARAM) String displayName,
                                  @RequestParam(JOURNAL_PARAM) String journalKey) {
    volumeId = volumeId.trim();
    validateNonEmpty(ID_PARAM, volumeId);
    DoiBasedIdentity id = DoiBasedIdentity.create(volumeId);
    volumeCrudService.create(id, displayName, journalKey);
    return reportCreated();
  }

  @RequestMapping(value = VOLUME_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws IOException {
    DoiBasedIdentity id = parse(request);
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    volumeCrudService.read(receiver, id, mf);
  }

}
