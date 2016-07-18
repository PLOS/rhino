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

import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.view.journal.IssueInputView;
import org.ambraproject.rhino.view.journal.VolumeInputView;
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
import java.io.IOException;

@Controller
public class VolumeCrudController extends RestController {

  @Autowired
  private VolumeCrudService volumeCrudService;
  @Autowired
  private IssueCrudService issueCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = "/volumes/{volumeDoi:.+}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    Doi volumeDoiObj = DoiEscaping.unescape(volumeDoi);

    // TODO: Look up journal; redirect to main service
    // TODO: Equivalent alias methods for other HTTP methods?
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi:.+}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("journalKey") String journalKey,
                   @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    Doi volumeDoiObj = DoiEscaping.unescape(volumeDoi);
    // TODO: Validate journalKey

    DoiBasedIdentity id = null; // TODO: Implement VolumeCrudService.read for identifier class
    volumeCrudService.read(id).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi:.+}", method = RequestMethod.PATCH)
  public void update(HttpServletRequest request, HttpServletResponse response,
                     @PathVariable("journalKey") String journalKey,
                     @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    Doi volumeDoiObj = DoiEscaping.unescape(volumeDoi);
    // TODO: Validate journalKey

    DoiBasedIdentity volumeId = null; // TODO: Implement services for identifier class
    VolumeInputView input = readJsonFromRequest(request, VolumeInputView.class);
    volumeCrudService.update(volumeId, input);

    volumeCrudService.read(volumeId).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi:.+}", method = RequestMethod.DELETE)
  public ResponseEntity<Object> delete(HttpServletRequest request,
                                       @PathVariable("journalKey") String journalKey,
                                       @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    Doi volumeDoiObj = DoiEscaping.unescape(volumeDoi);
    // TODO: Validate journalKey

    DoiBasedIdentity id = null; // TODO: Implement VolumeCrudService.delete for identifier class
    volumeCrudService.delete(id);
    return reportOk(id.getIdentifier());
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi}/issues", method = RequestMethod.POST)
  public ResponseEntity<String> createIssue(HttpServletRequest request,
                                            @PathVariable("journalKey") String journalKey,
                                            @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    Doi volumeDoiObj = DoiEscaping.unescape(volumeDoi);
    // TODO: Validate journalKey

    DoiBasedIdentity volumeId = null; // TODO: Implement IssueCrudService.create for identifier class

    IssueInputView input = readJsonFromRequest(request, IssueInputView.class);
    if (StringUtils.isBlank(input.getIssueUri())) {
      throw new RestClientException("issueUri required", HttpStatus.BAD_REQUEST);
    }

    DoiBasedIdentity issueId = issueCrudService.create(volumeId, input);
    return reportCreated(issueId.getIdentifier());
  }

}
