package org.ambraproject.rhino.rest.controller.abstr;

import com.google.common.base.Optional;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleRevisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Superclass for controllers that perform operations on article "REST nouns".
 */
public abstract class ArticleSpaceController extends RestController {

  protected static final String ARTICLE_ROOT = "/articles";

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

}
