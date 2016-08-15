package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.view.journal.JournalInputView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.OptionalInt;

@Controller
public class JournalCrudController extends RestController {

  @Autowired
  private JournalCrudService journalCrudService;
  @Autowired
  private CommentCrudService commentCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals", method = RequestMethod.GET)
  public void listJournals(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    journalCrudService.listJournals().respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable String journalKey,
                   @RequestParam(value = "currentIssue", required = false) String currentIssue)
      throws IOException {
    if (booleanParameter(currentIssue)) {
      journalCrudService.serveCurrentIssue(journalKey).respond(request, response, entityGson);
    } else {
      journalCrudService.serve(journalKey).respond(request, response, entityGson);
    }
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/journals/{journalKey}", method = RequestMethod.PATCH)
  public void update(HttpServletRequest request, HttpServletResponse response, @PathVariable String journalKey)
      throws IOException {
    JournalInputView input = readJsonFromRequest(request, JournalInputView.class);
    journalCrudService.update(journalKey, input);

    journalCrudService.serve(journalKey).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/journals/{journalKey}", method = RequestMethod.GET, params = "comments")
  public void getRecentComments(HttpServletRequest request, HttpServletResponse response,
                                @PathVariable String journalKey,
                                @RequestParam(value = "limit", required = false) Integer limit)
      throws IOException {
    OptionalInt limitObj = (limit == null) ? OptionalInt.empty() : OptionalInt.of(limit);
    commentCrudService.readRecentComments(journalKey, limitObj).respond(request, response, entityGson);
  }

}
