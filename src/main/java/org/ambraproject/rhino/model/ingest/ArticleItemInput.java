package org.ambraproject.rhino.model.ingest;

import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.identity.Doi;
import org.plos.crepo.model.input.RepoObjectInput;

import java.util.Map;
import java.util.Objects;

public class ArticleItemInput {

  private final Doi doi;
  private final ImmutableMap<String, RepoObjectInput> objects;
  private final String type;

  ArticleItemInput(Doi doi, Map<String, RepoObjectInput> objects, String type) {
    this.doi = Objects.requireNonNull(doi);
    this.objects = ImmutableMap.copyOf(objects);
    this.type = Objects.requireNonNull(type);
  }

  public Doi getDoi() {
    return doi;
  }

  public String getType() {
    return type;
  }

  public ImmutableMap<String, RepoObjectInput> getObjects() {
    return objects;
  }

}
