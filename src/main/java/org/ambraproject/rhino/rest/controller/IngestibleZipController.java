package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.impl.VersionedIngestionService;
import org.ambraproject.rhino.util.Archive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

@Controller
public class IngestibleZipController extends RestController {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private VersionedIngestionService versionedIngestionService;

  /**
   * Create an article based on a POST containing an article .zip archive file.
   *
   * @param response    response to the request
   * @param requestFile body of the archive param, with the encoded article .zip file
   * @throws java.io.IOException
   */
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/articles", method = RequestMethod.POST)
  public void zipUpload(HttpServletRequest request, HttpServletResponse response,
                        @RequestParam("archive") MultipartFile requestFile)
      throws IOException {

    String archiveName = requestFile.getOriginalFilename();
    ArticleIngestionIdentifier ingestionId;
    try (InputStream requestInputStream = requestFile.getInputStream();
         Archive archive = Archive.readZipFile(archiveName, requestInputStream)) {
      ingestionId = versionedIngestionService.ingest(archive);
    }
    response.setStatus(HttpStatus.CREATED.value());

    // Report the written data, as JSON, in the response.
    articleCrudService.readArticleMetadata(ingestionId).respond(request, response, entityGson);
  }

}
