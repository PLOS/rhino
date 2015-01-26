package org.ambraproject.rhino.config;

import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.ErrorType;
import org.plos.crepo.model.RepoCollection;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.service.contentRepo.ContentRepoService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
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
  public Map<String, Object> createRepoObject(RepoObject repoObject) {
    RepoObject previous = defaultBucket.objects.put(repoObject.getKey(), repoObject);
    if (previous != null) throw new RuntimeException("Overwrote object");
    return null;
  }

  @Override
  public Map<String, Object> autoCreateRepoObject(RepoObject repoObject) {
    defaultBucket.objects.put(repoObject.getKey(), repoObject);
    return null;
  }

  @Override
  public InputStream getLatestRepoObjStream(String key) {
    RepoObject repoObject = defaultBucket.objects.get(key);
    if (repoObject == null) {
      throw new ContentRepoException(ErrorType.ErrorFetchingObject,
          "StubContentRepoService's default bucket does not contain: " + key);
    }
    return new ByteArrayInputStream(repoObject.getByteContent());
  }

  @Override
  public Boolean deleteLatestRepoObj(String key) {
    return defaultBucket.objects.remove(key) != null;
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
  public Map<String, Object> createCollection(RepoCollection repoCollection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> versionCollection(RepoCollection repoCollection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Boolean deleteCollectionUsingVersionCks(String key, String versionChecksum) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Boolean deleteCollectionUsingVersionNumb(String key, int versionNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> getCollectionUsingVersionCks(String key, String versionChecksum) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> getCollectionUsingVersionNumber(String key, int versionNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> getCollectionUsingTag(String key, String tag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Map<String, Object>> getCollectionVersions(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Map<String, Object>> getCollections(int offset, int limit, boolean includeDeleted, String tag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Boolean hasXReproxy() {
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
  public URL[] getRepoObjRedirectURL(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public URL[] getRepoObjRedirectURL(String key, String versionChecksum) {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] getLatestRepoObjByteArray(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getRepoObjStreamUsingVersionCks(String key, String versionChecksum) {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] getRepoObjByteArrayUsingVersionCks(String key, String versionChecksum) {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getRepoObjStreamUsingVersionNum(String key, int versionNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] getRepoObjByteArrayUsingVersionNum(String key, int versionNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> getRepoObjMetaLatestVersion(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> getRepoObjMetaUsingVersionChecksum(String key, String versionChecksum) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> getRepoObjMetaUsingVersionNum(String key, int versionNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> getRepoObjMetaUsingTag(String key, String tag) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Map<String, Object>> getRepoObjVersions(String key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Boolean deleteRepoObjUsingVersionCks(String key, String versionChecksum) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Boolean deleteRepoObjUsingVersionNum(String key, int versionNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> versionRepoObject(RepoObject repoObject) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Map<String, Object>> getRepoObjects(int offset, int limit, boolean includeDeleted, String tag) {
    throw new UnsupportedOperationException();
  }
}
