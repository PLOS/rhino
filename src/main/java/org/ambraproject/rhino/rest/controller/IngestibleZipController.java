package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.impl.IngestionService;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.view.article.ArticleIngestionView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Controller
public class IngestibleZipController extends RestController {

  @Autowired
  private IngestionService ingestionService;
  @Autowired
  private ArticleIngestionView.Factory articleIngestionViewFactory;

  /**
   * Create an article based on a POST containing an article .zip archive file.
   *
   * @param requestFile body of the archive param, with the encoded article .zip file
   * @throws java.io.IOException
   */
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/articles", method = RequestMethod.POST)
  public ResponseEntity<?> zipUpload(@RequestParam("archive") MultipartFile requestFile)
      throws IOException {

    String ingestedFileName = requestFile.getOriginalFilename();
    ArticleIngestion ingestion;
    try (InputStream requestInputStream = requestFile.getInputStream();
        Archive archive = Archive.readZipFile(ingestedFileName, requestInputStream)) {
      ingestion = ingestionService.ingest(archive);
    } catch (ManifestXml.ManifestDataException e) {
      throw new RestClientException("Invalid manifest: " + e.getMessage(), HttpStatus.BAD_REQUEST, e);
    }

    // Report the written data, as JSON, in the response.
    ArticleIngestionView view = articleIngestionViewFactory.getView(ingestion);
    return ServiceResponse.reportCreated(view).asJsonResponse(entityGson);
  }

}
