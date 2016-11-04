package org.ambraproject.rhino.rest.controller;

import com.wordnik.swagger.annotations.ApiParam;
import org.ambraproject.rhino.model.ArticleCategoryAssignmentFlag;
import org.ambraproject.rhino.rest.response.TransientServiceResponse;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
public class CategoryReadController extends RestController {

  @Autowired
  private TaxonomyService taxonomyService;

  /**
   * Provides a utility endpoint for the publication workflow. The main use-case is to notify journal stakeholders when
   * new category flags are created.
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/categoryFlags", method = RequestMethod.GET, params = "created")
  public ResponseEntity<?> getFlagsCreatedOn(@ApiParam(value = "Date Format: yyyy-MM-dd")
                                             @RequestParam(value = "fromDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
                                             @ApiParam(value = "Date Format: yyyy-MM-dd")
                                             @RequestParam(value = "toDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate) throws IOException {
    List<ArticleCategoryAssignmentFlag> flags = taxonomyService.getFlagsCreatedOn(fromDate, toDate);
    return TransientServiceResponse.serveView(flags).asJsonResponse(entityGson);
  }
}
