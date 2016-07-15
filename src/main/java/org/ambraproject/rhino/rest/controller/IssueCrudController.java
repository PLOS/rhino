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
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.view.journal.IssueInputView;
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
public class IssueCrudController extends RestController {

  @Autowired
  private IssueCrudService issueCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = "/issues/{issueDoi}", method = RequestMethod.GET)
  public void read(@PathVariable("issueDoi") String issueDoi)
      throws IOException {
    Doi issueDoiObj = DoiEscaping.resolve(issueDoi);

    // TODO: Look up journal and volume; redirect to main service
    // TODO: Equivalent alias methods for other HTTP methods?
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi}/issues/{issueDoi}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("journalKey") String journalKey,
                   @PathVariable("volumeDoi") String volumeDoi,
                   @PathVariable("issueDoi") String issueDoi)
      throws IOException {
    Doi volumeDoiObj = DoiEscaping.resolve(volumeDoi);
    Doi issueDoiObj = DoiEscaping.resolve(issueDoi);
    // TODO: Validate journalKey and volumeDoiObj

    DoiBasedIdentity id = null; // TODO: Implement IssueCrudService.read for identifier class
    issueCrudService.read(id).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi}/issues/{issueDoi}", method = RequestMethod.PATCH)
  public void update(HttpServletRequest request, HttpServletResponse response,
                     @PathVariable("journalKey") String journalKey,
                     @PathVariable("volumeDoi") String volumeDoi,
                     @PathVariable("issueDoi") String issueDoi)
      throws IOException {
    Doi volumeDoiObj = DoiEscaping.resolve(volumeDoi);
    Doi issueDoiObj = DoiEscaping.resolve(issueDoi);
    // TODO: Validate journalKey and volumeDoiObj

    DoiBasedIdentity issueId = null; // TODO: Implement services for identifier class

    IssueInputView input = readJsonFromRequest(request, IssueInputView.class);
    issueCrudService.update(issueId, input);

    issueCrudService.read(issueId).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}/volumes/{volumeDoi}/issues/{issueDoi}", method = RequestMethod.DELETE)
  public ResponseEntity<Object> delete(HttpServletRequest request,
                                       @PathVariable("journalKey") String journalKey,
                                       @PathVariable("volumeDoi") String volumeDoi,
                                       @PathVariable("issueDoi") String issueDoi)
      throws IOException {
    Doi volumeDoiObj = DoiEscaping.resolve(volumeDoi);
    Doi issueDoiObj = DoiEscaping.resolve(issueDoi);
    // TODO: Validate journalKey and volumeDoiObj

    DoiBasedIdentity issueId = null; // TODO: Implement IssueCrudService.delete for identifier class
    issueCrudService.delete(issueId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

}
