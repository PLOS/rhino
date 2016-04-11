package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableMap;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObject;

import java.util.Map;

class ScholarlyWork {

  private final ImmutableMap<String, RepoObject> objects;

  ScholarlyWork(Map<String, RepoObject> objects) {
    this.objects = ImmutableMap.copyOf(objects);
  }

  // Create collection
  public RepoCollectionMetadata persistToCrepo() {
    // Write objects individually, capture their assigned IDs
    // Build JSON object matching keys to IDs
    // Create collection with those objects and the JSON
    // Write collection
    return null; // TODO: Implement
  }

  public void relate(ScholarlyWork work) {
    // TODO: Implement
  }
}
