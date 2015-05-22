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
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.view.journal.IssueInputView;
import org.ambraproject.rhino.view.journal.VolumeInputView;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class VolumeCrudController extends RestController {

  private static final String VOLUME_ROOT = "/volumes";

  @Autowired
  private VolumeCrudService volumeCrudService;
  @Autowired
  private IssueCrudService issueCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = VOLUME_ROOT, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(ID_PARAM) String id)
      throws IOException {
    volumeCrudService.read(DoiBasedIdentity.create(id)).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = VOLUME_ROOT, method = RequestMethod.PATCH)
  public void update(HttpServletRequest request, HttpServletResponse response,
                     @RequestParam(ID_PARAM) String id)
      throws IOException {
    DoiBasedIdentity volumeId = DoiBasedIdentity.create(id);
    VolumeInputView input = readJsonFromRequest(request, VolumeInputView.class);
    volumeCrudService.update(volumeId, input);

    volumeCrudService.read(volumeId).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = VOLUME_ROOT, method = RequestMethod.POST)
  public ResponseEntity<String> createIssue(HttpServletRequest request,
                                            @RequestParam(ID_PARAM) String id)
      throws IOException {
    DoiBasedIdentity volumeId = DoiBasedIdentity.create(id);

    IssueInputView input = readJsonFromRequest(request, IssueInputView.class);
    if (StringUtils.isBlank(input.getIssueUri())) {
      throw new RestClientException("issueUri required", HttpStatus.BAD_REQUEST);
    }

    DoiBasedIdentity issueId = issueCrudService.create(volumeId, input);
    return reportCreated(issueId.getIdentifier());
  }

}
