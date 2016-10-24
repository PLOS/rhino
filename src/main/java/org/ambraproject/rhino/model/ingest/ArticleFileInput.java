package org.ambraproject.rhino.model.ingest;

import org.plos.crepo.model.input.RepoObjectInput;

import java.util.Objects;

public class ArticleFileInput {

  private final String filename;
  private final RepoObjectInput object;

  ArticleFileInput(String filename, RepoObjectInput object) {
    this.filename = Objects.requireNonNull(filename);
    this.object = Objects.requireNonNull(object);
  }

  public String getFilename() {
    return filename;
  }

  public RepoObjectInput getObject() {
    return object;
  }

}
