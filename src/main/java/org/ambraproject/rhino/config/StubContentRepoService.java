package org.ambraproject.rhino.config;


import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.ErrorType;
import org.plos.crepo.model.RepoCollection;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.model.RepoVersionNumber;
import org.plos.crepo.model.RepoVersionTag;
import org.plos.crepo.service.ContentRepoService;

import java.io.ByteArrayInputStream;
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
      throw new ContentRepoException(ErrorType.ErrorFetchingObject,
          "StubContentRepoService's default bucket does not contain: " + key);
    }
    return new ByteArrayInputStream(repoObject.getByteContent());
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
  public Map<String, Object> getBucket(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> createBucket(String s) {
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
  public RepoObjectMetadata getLatestRepoObjectMetadata(String s) {
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
  public List<RepoObjectMetadata> getRepoObjectVersions(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean deleteRepoObject(RepoVersion repoVersion) {
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
  public List<RepoObjectMetadata> getRepoObjects(int i, int i1, boolean b, String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoCollectionMetadata createCollection(RepoCollection repoCollection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoCollectionMetadata versionCollection(RepoCollection repoCollection) {
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
  public RepoCollectionMetadata getCollection(RepoVersion repoVersion) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoCollectionMetadata getCollection(RepoVersionNumber repoVersionNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RepoCollectionMetadata getCollection(RepoVersionTag repoVersionTag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<RepoCollectionMetadata> getCollectionVersions(String s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<RepoCollectionMetadata> getCollections(int i, int i1, boolean b, String s) {
    throw new UnsupportedOperationException();
  }
}
