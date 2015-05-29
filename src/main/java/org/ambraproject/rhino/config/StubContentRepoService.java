package org.ambraproject.rhino.config;


import org.plos.crepo.exceptions.NotFoundException;
import org.plos.crepo.model.RepoCollection;
import org.plos.crepo.model.RepoCollectionList;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.model.RepoVersionNumber;
import org.plos.crepo.model.RepoVersionTag;
import org.plos.crepo.service.ContentRepoService;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Very narrow, quick-and-dirty stub implementation for ContentRepoService. Mocks minimal set of methods that Rhino
 * uses.
 */
public class StubContentRepoService implements ContentRepoService {

  private static class Bucket {
    private final Map<String, RepoObject> objects = new HashMap<>();
  }

  private final Map<String, Bucket> buckets = new HashMap<>();
  private final Bucket defaultBucket = new Bucket();

  /**
   * Empty the default bucket and delete all other buckets.
   */
  public void clear() {
    buckets.clear();
    defaultBucket.objects.clear();
  }


  @Override
  public RepoObjectMetadata createRepoObject(RepoObject repoObject) {
    RepoObject previous = defaultBucket.objects.put(repoObject.getKey(), repoObject);
    if (previous != null) throw new RuntimeException("Overwrote object");
    return null;
  }

  @Override
  public RepoObjectMetadata autoCreateRepoObject(RepoObject repoObject) {
    defaultBucket.objects.put(repoObject.getKey(), repoObject);
    return null;
  }

  @Override
  public InputStream getLatestRepoObject(String key) {
    RepoObject repoObject = defaultBucket.objects.get(key);
    if (repoObject == null) {
      throw new NotFoundException("StubContentRepoService's default bucket does not contain: " + key);
    }
    try {
      return repoObject.getContentAccessor().open();
    } catch (IOException e){
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean deleteLatestRepoObject(String key) {
    return defaultBucket.objects.remove(key) != null;
  }


  @Override
  public boolean hasXReproxy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> getRepoConfig() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> getRepoStatus() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Map<String, Object>> getBuckets() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> getBucket(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> createBucket(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getRepoObject(RepoVersion repoVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getRepoObject(RepoVersionNumber repoVersionNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoObjectMetadata getLatestRepoObjectMetadata(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoObjectMetadata getRepoObjectMetadata(RepoVersion repoVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoObjectMetadata getRepoObjectMetadata(RepoVersionNumber repoVersionNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoObjectMetadata getRepoObjectMetadata(RepoVersionTag repoVersionTag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<RepoObjectMetadata> getRepoObjectVersions(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean deleteRepoObject(RepoVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean deleteRepoObject(RepoVersionNumber repoVersionNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoObjectMetadata versionRepoObject(RepoObject repoObject) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<RepoObjectMetadata> getRepoObjects(int offset, int limit, boolean includeDeleted, String tag) {
    return null;
  }

  @Override
  public RepoCollectionList createCollection(RepoCollection repoCollection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoCollectionList versionCollection(RepoCollection repoCollection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoCollectionList autoCreateCollection(RepoCollection repoCollection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean deleteCollection(RepoVersion repoVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean deleteCollection(RepoVersionNumber repoVersionNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoCollectionList getCollection(RepoVersion repoVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoCollectionList getCollection(RepoVersionNumber repoVersionNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoCollectionList getCollection(RepoVersionTag repoVersionTag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<RepoCollectionList> getCollectionVersions(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<RepoCollectionMetadata> getCollections(int offset, int limit, boolean includeDeleted, String tag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoCollectionMetadata getLatestCollection(String s) {
    throw new UnsupportedOperationException();
  }
}