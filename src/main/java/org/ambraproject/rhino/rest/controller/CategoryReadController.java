package org.ambraproject.rhino.rest.controller;

import com.wordnik.swagger.annotations.ApiParam;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;

@Controller
public class CategoryReadController extends RestController {

  @Autowired
  private TaxonomyService taxonomyService;

  /**
   * Provides a utility endpoint for the publication workflow.
   * The main use-case is to notify journal stakeholders when new category flags are created.
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/categoryFlags", method = RequestMethod.GET, params = "created")
  public void getFlagsCreatedOn(HttpServletRequest request, HttpServletResponse response,
                                @ApiParam(value = "Date Format: yyyy-MM-dd")
                                @RequestParam(value = "fromDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
                                @ApiParam(value = "Date Format: yyyy-MM-dd")
                                @RequestParam(value = "toDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate) throws IOException {

    Transceiver.serveUntimestampedView(() -> taxonomyService.getFlagsCreatedOn(fromDate, toDate))
        .respond(request, response, entityGson);
  }
}
