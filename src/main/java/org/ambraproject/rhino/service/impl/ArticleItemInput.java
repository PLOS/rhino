package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.plos.crepo.model.input.RepoObjectInput;

import java.util.Map;
import java.util.Objects;

class ArticleItemInput {

  private final DoiBasedIdentity doi;
  private final ImmutableMap<String, RepoObjectInput> objects;
  private final String type;

  ArticleItemInput(DoiBasedIdentity doi, Map<String, RepoObjectInput> objects, String type) {
    this.doi = Objects.requireNonNull(doi);
    this.objects = ImmutableMap.copyOf(objects);
    this.type = Objects.requireNonNull(type);
  }

  public DoiBasedIdentity getDoi() {
    return doi;
  }

  public String getType() {
    return type;
  }

  public ImmutableMap<String, RepoObjectInput> getObjects() {
    return objects;
  }

}
