package org.ambraproject.rhino.rest.controller.abstr;

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.service.CommentCrudService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

/**
 * Superclass for controllers that perform operations on comment's "REST nouns".
 */
public abstract class CommentSpaceController extends DoiBasedCrudController {

  protected static final String COMMENT_META_ROOT = "/comments";
  protected static final String COMMENT_META_NAMESPACE = COMMENT_META_ROOT + "/";
  protected static final String COMMENT_META_TEMPLATE = COMMENT_META_NAMESPACE + "/**";

  protected static final String FLAGS_META_ROOT = "/flags";
  protected static final String FLAGS_META_TEMPLATE = FLAGS_META_ROOT + "/{flagId}";

  @Autowired
  protected CommentCrudService commentCrudService;

  @Override
  protected final String getNamespacePrefix() {
    return COMMENT_META_NAMESPACE;
  }

  @Override
  protected final ArticleIdentity parse(HttpServletRequest request) {
    return ArticleIdentity.create(getIdentifier(request));
  }

}
