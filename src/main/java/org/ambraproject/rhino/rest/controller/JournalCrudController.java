package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.identity.VolumeIdentifier;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.view.journal.JournalInputView;
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
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.OptionalInt;

@Controller
public class JournalCrudController extends RestController {

  private static final String JOURNAL_ROOT = "/journals";
  private static final String JOURNAL_TEMPLATE = JOURNAL_ROOT + "/{journalKey}";

  @Autowired
  private JournalCrudService journalCrudService;
  @Autowired
  private VolumeCrudService volumeCrudService;
  @Autowired
  private CommentCrudService commentCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = JOURNAL_ROOT, method = RequestMethod.GET)
  public void listJournals(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    journalCrudService.listJournals().respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = JOURNAL_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable String journalKey,
                   @RequestParam(value = "currentIssue", required = false) String currentIssue)
      throws IOException {
    if (booleanParameter(currentIssue)) {
      journalCrudService.readCurrentIssue(journalKey).respond(request, response, entityGson);
    } else {
      journalCrudService.read(journalKey).respond(request, response, entityGson);
    }
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = JOURNAL_TEMPLATE, method = RequestMethod.POST)
  public ResponseEntity<String> createVolume(HttpServletRequest request, @PathVariable String journalKey)
      throws IOException {
    VolumeInputView input = readJsonFromRequest(request, VolumeInputView.class);
    if (StringUtils.isBlank(input.getVolumeUri())) {
      throw new RestClientException("volumeUri required", HttpStatus.BAD_REQUEST);
    }

    VolumeIdentifier volumeId = volumeCrudService.create(journalKey, input);
    return reportCreated(volumeId.getVolumeUri());
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = JOURNAL_TEMPLATE, method = RequestMethod.PATCH)
  public void update(HttpServletRequest request, HttpServletResponse response, @PathVariable String journalKey)
      throws IOException {
    JournalInputView input = readJsonFromRequest(request, JournalInputView.class);
    journalCrudService.update(journalKey, input);

    journalCrudService.read(journalKey).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = JOURNAL_TEMPLATE, method = RequestMethod.GET, params = "comments")
  public void getRecentComments(HttpServletRequest request, HttpServletResponse response,
                                @PathVariable String journalKey,
                                @RequestParam(value = "limit", required = false) Integer limit)
      throws IOException {
    OptionalInt limitObj = (limit == null) ? OptionalInt.empty() : OptionalInt.of(limit);
    commentCrudService.readRecentComments(journalKey, limitObj).respond(request, response, entityGson);
  }

}
