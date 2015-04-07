package org.ambraproject.rhino.rest.controller.abstr;

import com.google.common.base.Optional;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Superclass for controllers that perform operations on article "REST nouns".
 */
public abstract class ArticleSpaceController extends RestController {

  protected static final String ARTICLE_ROOT = "/articles";

  @Autowired
  protected ArticleCrudService articleCrudService;

  protected ArticleIdentity parse(String id, Integer versionNumber, Integer revisionNumber) {
    if (revisionNumber == null) {
      return new ArticleIdentity(id, Optional.fromNullable(versionNumber));
    } else {
      if (versionNumber != null) {
        throw new RestClientException("Cannot specify version and revision", HttpStatus.NOT_FOUND);
      }
      return null; // TODO: Look up by revision and return with correct version
    }
  }

}
