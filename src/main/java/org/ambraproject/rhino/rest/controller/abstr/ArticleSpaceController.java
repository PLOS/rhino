package org.ambraproject.rhino.rest.controller.abstr;

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

/**
 * Superclass for controllers that perform operations on article "REST nouns".
 */
public abstract class ArticleSpaceController extends RestController {

  protected static final String ARTICLE_ROOT = "/articles";

  @Autowired
  protected ArticleCrudService articleCrudService;

}
