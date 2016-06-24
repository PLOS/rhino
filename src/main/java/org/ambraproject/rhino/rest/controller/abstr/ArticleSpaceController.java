package org.ambraproject.rhino.rest.controller.abstr;

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

/**
 * Superclass for controllers that perform operations on article "REST nouns".
 */
public abstract class ArticleSpaceController extends DoiBasedCrudController {

  protected static final String ARTICLE_ROOT = "/articles";
  protected static final String ARTICLE_NAMESPACE = ARTICLE_ROOT + '/';
  protected static final String ARTICLE_TEMPLATE = ARTICLE_NAMESPACE + "**";

  protected static final String SYNDICATION_ROOT = ARTICLE_TEMPLATE + "/syndications";
  protected static final String SYNDICATION_NAMESPACE = SYNDICATION_ROOT + '/';

  @Autowired
  protected ArticleCrudService articleCrudService;

  @Override
  protected final String getNamespacePrefix() {
    return ARTICLE_NAMESPACE;
  }

  @Override
  protected final ArticleIdentity parse(HttpServletRequest request) {
    return ArticleIdentity.create(getIdentifier(request));
  }

}
