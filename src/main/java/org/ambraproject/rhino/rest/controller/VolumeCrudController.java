/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.rest.controller;

import com.wordnik.swagger.annotations.ApiImplicitParam;
import org.ambraproject.rhino.identity.VolumeIdentifier;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.view.journal.VolumeInputView;
import org.ambraproject.rhino.view.journal.VolumeOutputView;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class VolumeCrudController extends RestController {

  @Autowired
  private VolumeCrudService volumeCrudService;
  @Autowired
  private JournalCrudService journalCrudService;
  @Autowired
  private VolumeOutputView.Factory volumeOutputViewFactory;

  private static VolumeIdentifier getVolumeId(String volumeDoi) {
    return VolumeIdentifier.create(DoiEscaping.unescape(volumeDoi));
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/volumes/{volumeDoi:.+}", method = RequestMethod.GET)
  public ResponseEntity<?> read(@RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Date ifModifiedSince,
                   HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    VolumeIdentifier volumeId = getVolumeId(volumeDoi);
    return volumeCrudService.serveVolume(volumeId).getIfModified(ifModifiedSince).asJsonResponse(entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}/volumes", method = RequestMethod.GET)
  public ResponseEntity<?> readVolumesForJournal(@PathVariable("journalKey") String journalKey)
      throws IOException {
    Journal journal = journalCrudService.readJournal(journalKey);
    List<VolumeOutputView> views = journal.getVolumes().stream()
        .map(volumeOutputViewFactory::getView)
        .collect(Collectors.toList());
    return ServiceResponse.serveView(views).asJsonResponse(entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi:.+}", method = RequestMethod.GET)
  public ResponseEntity<?> read(@RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Date ifModifiedSince,
                                @PathVariable("journalKey") String journalKey,
                                @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    // TODO: Validate journalKey
    VolumeIdentifier volumeId = getVolumeId(volumeDoi);
    return volumeCrudService.serveVolume(volumeId).getIfModified(ifModifiedSince).asJsonResponse(entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}/volumes", method = RequestMethod.POST)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "VolumeInputView",
      value = "example: {\"doi\": \"10.1371/volume.pmed.v01\", \"displayName\": \"2004\"}")
  public ResponseEntity<?> create(HttpServletRequest request, @PathVariable String journalKey)
      throws IOException {
    VolumeInputView input = readJsonFromRequest(request, VolumeInputView.class);
    if (StringUtils.isBlank(input.getDoi())) {
      throw new RestClientException("Volume DOI required", HttpStatus.BAD_REQUEST);
    }

    Volume volume = volumeCrudService.create(journalKey, input);
    return ServiceResponse.reportCreated(volumeOutputViewFactory.getView(volume)).asJsonResponse(entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi:.+}", method = RequestMethod.PATCH)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "VolumeInputView",
      value = "example #1: {\"doi\": \"10.1371/volume.pmed.v01\"}<br>" +
          "example #2: {\"displayName\": \"2004\"}")
  public ResponseEntity<?> update(HttpServletRequest request,
                                  @PathVariable("journalKey") String journalKey,
                                  @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    // TODO: Validate journalKey
    VolumeIdentifier volumeId = getVolumeId(volumeDoi);
    VolumeInputView input = readJsonFromRequest(request, VolumeInputView.class);
    Volume updated = volumeCrudService.update(volumeId, input);
    VolumeOutputView view = volumeOutputViewFactory.getView(updated);
    return ServiceResponse.serveView(view).asJsonResponse(entityGson);
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
