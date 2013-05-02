package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.JournalReadService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class JournalReadController extends RestController {

  private static final String JOURNAL_ROOT = "/journals";

  @Autowired
  private JournalReadService journalReadService;

  @RequestMapping(value = JOURNAL_ROOT, method = RequestMethod.GET)
  public void listJournals(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws IOException {
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    journalReadService.read(receiver, mf);
  }

}
