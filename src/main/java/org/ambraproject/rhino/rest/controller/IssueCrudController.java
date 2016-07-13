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

import org.ambraproject.rhino.identity.IssueIdentifier;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.view.journal.IssueInputView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class IssueCrudController extends DoiBasedCrudController {

  private static final String ISSUE_NAMESPACE = "/issues/";
  private static final String ISSUE_TEMPLATE = ISSUE_NAMESPACE + "**";

  @Override
  protected String getNamespacePrefix() {
    return ISSUE_NAMESPACE;
  }

  @Autowired
  private IssueCrudService issueCrudService;

  protected final IssueIdentifier parseIssueId(HttpServletRequest request) {
    return IssueIdentifier.create(getIdentifier(request));
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = ISSUE_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    IssueIdentifier issueId = parseIssueId(request);
    issueCrudService.read(issueId).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = ISSUE_TEMPLATE, method = RequestMethod.PATCH)
  public void update(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    IssueIdentifier issueId = parseIssueId(request);
    IssueInputView input = readJsonFromRequest(request, IssueInputView.class);
    issueCrudService.update(issueId, input);

    issueCrudService.read(issueId).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = ISSUE_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<Object> delete(HttpServletRequest request)
      throws IOException {
    IssueIdentifier issueId = parseIssueId(request);
    issueCrudService.delete(issueId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

}
