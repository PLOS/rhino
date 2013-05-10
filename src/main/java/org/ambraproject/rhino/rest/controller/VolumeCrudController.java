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
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.ambraproject.rhino.view.journal.IssueInputView;
import org.ambraproject.rhino.view.journal.VolumeInputView;
import org.apache.commons.lang.StringUtils;
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

  @Override
  protected String getNamespacePrefix() {
    return VOLUME_NAMESPACE;
  }

  @Autowired
  private VolumeCrudService volumeCrudService;
  @Autowired
  private IssueCrudService issueCrudService;


  @RequestMapping(value = VOLUME_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws IOException {
    DoiBasedIdentity id = parse(request);
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    volumeCrudService.read(receiver, id, mf);
  }

  @RequestMapping(value = VOLUME_TEMPLATE, method = RequestMethod.PATCH)
  public void update(HttpServletRequest request, HttpServletResponse response,
                     @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws IOException {
    DoiBasedIdentity volumeId = parse(request);
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    VolumeInputView input = readJsonFromRequest(request, VolumeInputView.class);
    volumeCrudService.update(volumeId, input);

    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    volumeCrudService.read(receiver, volumeId, mf);
  }

  @RequestMapping(value = VOLUME_TEMPLATE, method = RequestMethod.POST)
  public ResponseEntity<String> createIssue(HttpServletRequest request) throws IOException {
    DoiBasedIdentity volumeId = parse(request);

    IssueInputView input = readJsonFromRequest(request, IssueInputView.class);
    if (StringUtils.isBlank(input.getIssueUri())) {
      throw new RestClientException("issueUri required", HttpStatus.BAD_REQUEST);
    }

    DoiBasedIdentity issueId = issueCrudService.create(volumeId, input);
    return reportCreated(issueId.getIdentifier());
  }

}
