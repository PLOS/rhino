package org.ambraproject.rhino.rest.controller.abstr;

import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleRevisionService;
import org.ambraproject.rhino.service.IdentityService;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Superclass for controllers that perform operations on article "REST nouns".
 */
public abstract class ArticleSpaceController extends RestController {

  protected static final String ARTICLE_ROOT = "/articles";

  @Autowired
  protected ContentRepoService contentRepoService;
  @Autowired
  protected ArticleCrudService articleCrudService;
  @Autowired
  private IdentityService identityService;

  protected ArticleIdentity parse(String id, Integer versionNumber, Integer revisionNumber) {
    return identityService.parseArticleId(id, versionNumber, revisionNumber);
  }

  protected void streamRepoObject(HttpServletResponse response, RepoObjectMetadata objectVersion) throws IOException {
    // TODO: Respect headers, reproxying, etc. This is just prototype code.
    // Unify with org.ambraproject.rhino.rest.controller.AssetFileCrudController.read
    response.setStatus(HttpStatus.OK.value());

    try (InputStream fileStream = contentRepoService.getRepoObject(objectVersion.getVersion());
         OutputStream responseStream = response.getOutputStream()) {
      ByteStreams.copy(fileStream, responseStream);
    }
  }


}
