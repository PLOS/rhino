package org.ambraproject.rhino.rest.controller.abstr;

import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleRevisionService;
import org.plos.crepo.exceptions.NotFoundException;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.ambraproject.rhino.service.impl.AmbraService.entityNotFound;

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
  private ArticleRevisionService articleRevisionService;

  protected ArticleIdentity parse(String id, Integer versionNumber, Integer revisionNumber) {
    if (revisionNumber == null) {
      return new ArticleIdentity(id, Optional.fromNullable(versionNumber));
    } else {
      int revisionVersionNumber = articleRevisionService.findVersionNumber(ArticleIdentity.create(id), revisionNumber);
      if (versionNumber != null && versionNumber != revisionVersionNumber) {
        String message = String.format("Mismatch between version and revision " +
                "(provided v=%d&r=%d; correct revision for v=%d is r=%d)",
            versionNumber, revisionNumber, versionNumber, revisionVersionNumber);
        throw new RestClientException(message, HttpStatus.NOT_FOUND);
      }
      return new ArticleIdentity(id, Optional.of(revisionVersionNumber));
    }
  }

  protected void streamRepoObject(HttpServletResponse response, RepoObjectMetadata objectVersion) throws IOException {
    // TODO: Respect headers, reproxying, etc. This is just prototype code.
    // Unify with org.ambraproject.rhino.rest.controller.AssetFileCrudController.read
    response.setStatus(HttpStatus.OK.value());

    try (InputStream fileStream = contentRepoService.getRepoObject(objectVersion.getVersion());
         OutputStream responseStream = response.getOutputStream()) {
      ByteStreams.copy(fileStream, responseStream);
    } catch (NotFoundException nfe) {
      throw entityNotFound(nfe.getMessage());
    }
  }


}
