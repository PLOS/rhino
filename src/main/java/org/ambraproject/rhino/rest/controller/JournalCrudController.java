package org.ambraproject.rhino.rest.controller;

import com.wordnik.swagger.annotations.ApiImplicitParam;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.journal.IssueOutputView;
import org.ambraproject.rhino.view.journal.JournalInputView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class JournalCrudController extends RestController {

  @Autowired
  private JournalCrudService journalCrudService;
  @Autowired
  private IssueOutputView.Factory issueOutputViewFactory;

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals", method = RequestMethod.GET)
  public void listJournals(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    journalCrudService.listJournals().respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable String journalKey)
      throws IOException {
    journalCrudService.serve(journalKey).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}", method = RequestMethod.PATCH)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "JournalInputView",
      value= "example: {\"currentIssueDoi\": \"10.1371/issue.pmed.v13.i09\"}")
  public void update(HttpServletRequest request, HttpServletResponse response, @PathVariable String journalKey)
      throws IOException {
    JournalInputView input = readJsonFromRequest(request, JournalInputView.class);
    journalCrudService.update(journalKey, input);

    journalCrudService.serve(journalKey).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}/currentIssue", method = RequestMethod.GET)
  public void readCurrentIssue(HttpServletRequest request, HttpServletResponse response,
                               @PathVariable String journalKey)
      throws IOException {
    Journal journal = journalCrudService.readJournal(journalKey);
    Transceiver.serveTimestampedView(journal, () ->
        issueOutputViewFactory.getCurrentIssueViewFor(journal)
            .orElseThrow(() -> new RestClientException("Current issue is not set for " + journalKey, HttpStatus.NOT_FOUND)))
        .respond(request, response, entityGson);
  }
}
