package org.ambraproject.rhino.rest.controller;

import com.wordnik.swagger.annotations.ApiImplicitParam;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.TransientServiceResponse;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.view.journal.IssueOutputView;
import org.ambraproject.rhino.view.journal.JournalInputView;
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
import java.io.IOException;
import java.util.Date;

@Controller
public class JournalCrudController extends RestController {

  @Autowired
  private JournalCrudService journalCrudService;
  @Autowired
  private IssueOutputView.Factory issueOutputViewFactory;

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals", method = RequestMethod.GET)
  public ResponseEntity<?> listJournals(@RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Date ifModifiedSince)
      throws IOException {
    return journalCrudService.listJournals().asJsonResponse(ifModifiedSince, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}", method = RequestMethod.GET)
  public ResponseEntity<?> read(@RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Date ifModifiedSince,
                                @PathVariable String journalKey)
      throws IOException {
    return journalCrudService.serve(journalKey).asJsonResponse(ifModifiedSince, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}", method = RequestMethod.PATCH)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "JournalInputView",
      value = "example: {\"currentIssueDoi\": \"10.1371/issue.pmed.v13.i09\"}")
  public ResponseEntity<?> update(@RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Date ifModifiedSince,
                                  HttpServletRequest request, @PathVariable String journalKey)
      throws IOException {
    JournalInputView input = readJsonFromRequest(request, JournalInputView.class);
    journalCrudService.update(journalKey, input);

    return journalCrudService.serve(journalKey).asJsonResponse(ifModifiedSince, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}/currentIssue", method = RequestMethod.GET)
  public ResponseEntity<?> readCurrentIssue(@PathVariable String journalKey)
      throws IOException {
    Journal journal = journalCrudService.readJournal(journalKey);
    IssueOutputView view = issueOutputViewFactory.getCurrentIssueViewFor(journal)
        .orElseThrow(() -> new RestClientException("Current issue is not set for " + journalKey, HttpStatus.NOT_FOUND));
    return TransientServiceResponse.serveView(view).asJsonResponse(entityGson);
  }
}
