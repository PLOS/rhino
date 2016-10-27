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
import org.ambraproject.rhino.identity.IssueIdentifier;
import org.ambraproject.rhino.identity.VolumeIdentifier;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.journal.IssueInputView;
import org.ambraproject.rhino.view.journal.IssueOutputView;
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
import java.util.stream.Collectors;

@Controller
public class IssueCrudController extends RestController {

  @Autowired
  private IssueCrudService issueCrudService;
  @Autowired
  private VolumeCrudService volumeCrudService;
  @Autowired
  private IssueOutputView.Factory issueOutputViewFactory;

  private IssueIdentifier getIssueId(String issueDoi) {
    return IssueIdentifier.create(DoiEscaping.unescape(issueDoi));
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/issues/{issueDoi:.+}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("issueDoi") String issueDoi)
      throws IOException {
    IssueIdentifier issueId = getIssueId(issueDoi);
    issueCrudService.serveIssue(issueId).respond(request, response, entityGson);

    // TODO: Equivalent alias methods for other HTTP methods?
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/issues/{issueDoi:.+}/contents", method = RequestMethod.GET)
  public void readArticlesInIssue(HttpServletRequest request, HttpServletResponse response,
                                  @PathVariable("issueDoi") String issueDoi)
      throws IOException {
    IssueIdentifier issueId = getIssueId(issueDoi);
    Issue issue = issueCrudService.readIssue(issueId);
    Transceiver.serveTimestampedView(issue,
        () -> issueOutputViewFactory.getIssueArticlesView(issue))
        .respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi}/issues", method = RequestMethod.GET)
  public void readIssuesForVolume(HttpServletRequest request, HttpServletResponse response,
                                  @PathVariable("journalKey") String journalKey,
                                  @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    // TODO: Validate journalKey
    VolumeIdentifier volumeId = VolumeIdentifier.create(DoiEscaping.unescape(volumeDoi));
    Volume volume = volumeCrudService.readVolume(volumeId);
    Transceiver.serveTimestampedView(volume,
        () -> volume.getIssues().stream()
            .map(issueOutputViewFactory::getView)
            .collect(Collectors.toList()))
        .respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi}/issues/{issueDoi:.+}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("journalKey") String journalKey,
                   @PathVariable("volumeDoi") String volumeDoi,
                   @PathVariable("issueDoi") String issueDoi)
      throws IOException {
    // TODO: Validate journalKey and volumeDoi
    read(request, response, issueDoi);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi}/issues/{issueDoi:.+}/contents", method = RequestMethod.GET)
  public void readArticlesInIssue(HttpServletRequest request, HttpServletResponse response,
                                  @PathVariable("journalKey") String journalKey,
                                  @PathVariable("volumeDoi") String volumeDoi,
                                  @PathVariable("issueDoi") String issueDoi)
      throws IOException {
    // TODO: Validate journalKey and volumeDoi
    readArticlesInIssue(request, response, issueDoi);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi}/issues", method = RequestMethod.POST)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "IssueInputView",
      value= "example: {\"doi\": \"10.1371/issue.pbio.v02.i07\", " +
          "\"displayName\": \"July\", " +
          "\"imageArticleDoi\": \"10.1371/image.pbio.v02.i07\", " +
          "\"articleOrder\": [\"10.1371/journal.pbio.0020213\", \"10.1371/journal.pbio.0020214\", " +
          "\"10.1371/journal.pbio.0020228\"]}")
  public ResponseEntity<String> create(HttpServletRequest request,
                                       @PathVariable("journalKey") String journalKey,
                                       @PathVariable("volumeDoi") String volumeDoi)
      throws IOException {
    // TODO: Validate journalKey
    VolumeIdentifier volumeId = VolumeIdentifier.create(DoiEscaping.unescape(volumeDoi));
    IssueInputView input = readJsonFromRequest(request, IssueInputView.class);
    if (StringUtils.isBlank(input.getDoi())) {
      throw new RestClientException("issueUri required", HttpStatus.BAD_REQUEST);
    }

    Issue issue = issueCrudService.create(volumeId, input);
    return reportCreated(issueOutputViewFactory.getView(issue));
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi}/issues/{issueDoi:.+}", method = RequestMethod.PATCH)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "IssueInputView",
      value= "example #1: {\"displayName\": \"July\"}<br>" +
          "example #2: {\"imageArticleDoi\": \"10.1371/image.pbio.v02.i07\"}<br>" +
          "example #3: {\"articleOrder\": [\"10.1371/journal.pbio.0020213\", \"10.1371/journal.pbio.0020214\", " +
          "\"10.1371/journal.pbio.0020228\"]}")
  public void update(HttpServletRequest request, HttpServletResponse response,
                     @PathVariable("journalKey") String journalKey,
                     @PathVariable("volumeDoi") String volumeDoi,
                     @PathVariable("issueDoi") String issueDoi)
      throws IOException {
    // TODO: Validate journalKey and volumeDoiObj
    IssueIdentifier issueId = getIssueId(issueDoi);
    IssueInputView input = readJsonFromRequest(request, IssueInputView.class);
    issueCrudService.update(issueId, input);

    issueCrudService.serveIssue(issueId).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi}/issues/{issueDoi:.+}", method = RequestMethod.DELETE)
  public ResponseEntity<Object> delete(HttpServletRequest request,
                                       @PathVariable("journalKey") String journalKey,
                                       @PathVariable("volumeDoi") String volumeDoi,
                                       @PathVariable("issueDoi") String issueDoi)
      throws IOException {
    // TODO: Validate journalKey and volumeDoiObj
    IssueIdentifier issueId = getIssueId(issueDoi);
    issueCrudService.delete(issueId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

}
