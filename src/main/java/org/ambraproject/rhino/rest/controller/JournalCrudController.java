package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.JournalReadService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.ambraproject.rhino.view.journal.VolumeInputView;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class JournalCrudController extends RestController {

  private static final String JOURNAL_ROOT = "/journals";
  private static final String JOURNAL_TEMPLATE = JOURNAL_ROOT + "/{journalKey}";

  @Autowired
  private JournalReadService journalReadService;
  @Autowired
  private VolumeCrudService volumeCrudService;

  @RequestMapping(value = JOURNAL_ROOT, method = RequestMethod.GET)
  public void listJournals(HttpServletResponse response,
                           @RequestParam(value = JSONP_CALLBACK_PARAM, required = false) String jsonp,
                           @RequestHeader(value = ACCEPT_REQUEST_HEADER, required = false) String accept)
      throws IOException {
    MetadataFormat mf = MetadataFormat.getFromAcceptHeader(accept);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(jsonp, response);
    journalReadService.listJournals(receiver, mf);
  }

  @RequestMapping(value = JOURNAL_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletResponse response,
                   @PathVariable String journalKey,
                   @RequestParam(value = "inTheNewsArticles", required = false) String inTheNewsArticles,
                   @RequestParam(value = JSONP_CALLBACK_PARAM, required = false) String jsonp,
                   @RequestHeader(value = ACCEPT_REQUEST_HEADER, required = false) String accept)
      throws IOException {
    MetadataFormat mf = MetadataFormat.getFromAcceptHeader(accept);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(jsonp, response);
    if (booleanParameter(inTheNewsArticles)) {
      journalReadService.readInTheNewsArticles(receiver, journalKey, mf);
    } else {
      journalReadService.read(receiver, journalKey, mf);
    }
  }

  @RequestMapping(value = JOURNAL_TEMPLATE, method = RequestMethod.POST)
  public ResponseEntity<String> createVolume(HttpServletRequest request, @PathVariable String journalKey)
      throws IOException {
    VolumeInputView input = readJsonFromRequest(request, VolumeInputView.class);
    if (StringUtils.isBlank(input.getVolumeUri())) {
      throw new RestClientException("volumeUri required", HttpStatus.BAD_REQUEST);
    }

    DoiBasedIdentity volumeId = volumeCrudService.create(journalKey, input);
    return reportCreated(volumeId.getIdentifier());
  }

}
