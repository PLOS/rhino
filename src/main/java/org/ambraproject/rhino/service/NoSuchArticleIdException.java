package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;

public class NoSuchArticleIdException extends RuntimeException {
  public NoSuchArticleIdException(ArticleRevisionIdentifier articleIdentifier) {
    super("No such article: " + articleIdentifier);
  }

  public NoSuchArticleIdException(ArticleIdentifier articleIdentifier) {
    super("No such article: " + articleIdentifier);
  }

  public NoSuchArticleIdException(ArticleIngestionIdentifier articleIdentifier) {
    super("No such article: " + articleIdentifier);
  }
}