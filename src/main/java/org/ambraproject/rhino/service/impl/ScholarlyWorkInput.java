package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.plos.crepo.model.RepoObject;

import java.util.Map;
import java.util.Objects;

class ScholarlyWorkInput {

  private final DoiBasedIdentity doi;
  private final ImmutableMap<String, RepoObject> objects;
  private final String type;

  ScholarlyWorkInput(DoiBasedIdentity doi, Map<String, RepoObject> objects, String type) {
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

  public ImmutableMap<String, RepoObject> getObjects() {
    return objects;
  }

}
