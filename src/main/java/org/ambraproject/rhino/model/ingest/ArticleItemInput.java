package org.ambraproject.rhino.model.ingest;

import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.identity.Doi;

import java.util.Map;
import java.util.Objects;

public class ArticleItemInput {

  private final Doi doi;
  private final ImmutableMap<String, ArticleFileInput> files;
  private final String type;

  ArticleItemInput(Doi doi, Map<String, ArticleFileInput> files, String type) {
    this.doi = Objects.requireNonNull(doi);
    this.files = ImmutableMap.copyOf(files);
    this.type = Objects.requireNonNull(type);
  }

  public Doi getDoi() {
    return doi;
  }

  public String getType() {
    return type;
  }

  public ImmutableMap<String, ArticleFileInput> getFiles() {
    return files;
  }

}
