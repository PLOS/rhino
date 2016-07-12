package org.ambraproject.rhino.view.article.author;

import java.util.Objects;

public class AuthorRole {
  
  private final String content;
  private final String type;

  public AuthorRole(String content, String type) {
    this.content = Objects.requireNonNull(content);
    this.type = type; // nullable
  }
  
}
