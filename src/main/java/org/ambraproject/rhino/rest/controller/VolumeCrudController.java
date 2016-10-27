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

import com.wordnik.swagger.annotations.ApiImplicitParam;
import org.ambraproject.rhino.identity.VolumeIdentifier;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.journal.VolumeInputView;
import org.ambraproject.rhino.view.journal.VolumeOutputView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.stream.Collectors;

@Controller
public class VolumeCrudController extends RestController {

  @Autowired
  private VolumeCrudService volumeCrudService;
  @Autowired
  private JournalCrudService journalCrudService;

  private static VolumeIdentifier getVolumeId(String volumeDoi) {
    return VolumeIdentifier.create(DoiEscaping.unescape(volumeDoi));
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/volumes/{volumeDoi:.+}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    VolumeIdentifier volumeId = getVolumeId(volumeDoi);
    // TODO: Look up journal; redirect to main service
    // TODO: Equivalent alias methods for other HTTP methods?
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}/volumes", method = RequestMethod.GET)
  public void readVolumesForJournal(HttpServletRequest request, HttpServletResponse response,
                                    @PathVariable("journalKey") String journalKey)
      throws IOException {
    Journal journal = journalCrudService.readJournal(journalKey);
    Transceiver.serveTimestampedView(journal,
        () -> journal.getVolumes().stream()
            .map(VolumeOutputView::getView)
            .collect(Collectors.toList()))
        .respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi:.+}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("journalKey") String journalKey,
                   @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    // TODO: Validate journalKey
    VolumeIdentifier volumeId = getVolumeId(volumeDoi);
    volumeCrudService.serveVolume(volumeId).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}/volumes", method = RequestMethod.POST)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "VolumeInputView",
      value= "example: {\"doi\": \"10.1371/volume.pmed.v01\", \"displayName\": \"2004\"}")
  public ResponseEntity<String> create(HttpServletRequest request, @PathVariable String journalKey)
      throws IOException {
    VolumeInputView input = readJsonFromRequest(request, VolumeInputView.class);
    if (StringUtils.isBlank(input.getDoi())) {
      throw new RestClientException("Volume DOI required", HttpStatus.BAD_REQUEST);
    }

    Volume volume = volumeCrudService.create(journalKey, input);
    return reportCreated(VolumeOutputView.getView(volume));
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi:.+}", method = RequestMethod.PATCH)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "VolumeInputView",
      value= "example #1: {\"doi\": \"10.1371/volume.pmed.v01\"}<br>" +
      "example #2: {\"displayName\": \"2004\"}")
  public ResponseEntity<?> update(HttpServletRequest request, HttpServletResponse response,
                                  @PathVariable("journalKey") String journalKey,
                                  @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    // TODO: Validate journalKey
    VolumeIdentifier volumeId = getVolumeId(volumeDoi);
    VolumeInputView input = readJsonFromRequest(request, VolumeInputView.class);
    Volume updated = volumeCrudService.update(volumeId, input);
    return reportUpdated(VolumeOutputView.getView(updated));
  }

  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi:.+}", method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request,
                                  @PathVariable("journalKey") String journalKey,
                                  @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    // TODO: Validate journalKey
    VolumeIdentifier volumeId = getVolumeId(volumeDoi);
    volumeCrudService.delete(volumeId);
    return reportDeleted(volumeId.getDoi().getName());
  }

}
