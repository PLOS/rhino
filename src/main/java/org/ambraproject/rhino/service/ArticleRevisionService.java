package org.ambraproject.rhino.service;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.impl.AmbraService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

public class ArticleRevisionService extends AmbraService {

  @Autowired
  private ContentRepoService versionedContentRepoService;


  public Transceiver readVersion(ArticleIdentity articleIdentity, UUID uuid) {
    final RepoVersion version = RepoVersion.create(articleIdentity.getIdentifier(), uuid);
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        RepoCollectionMetadata collection = versionedContentRepoService.getCollection(version);

        // TODO: Implement a view. Don't actually want to expose UUIDs, etc.
        return collection.getJsonUserMetadata().orNull();
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  /**
   * Return the metadata for the object in a collection with a given key.
   *
   * @param collectionVersion the collection's identifier
   * @param objectKey         the key of the object to search for
   * @return the metadata of the object, or {@code null} if no object with the key is in the collection
   * @throws IllegalArgumentException if two or more objects in the collection have the given key
   */
  private RepoObjectMetadata findObjectInCollection(RepoVersion collectionVersion, String objectKey) {
    Preconditions.checkNotNull(objectKey);
    RepoCollectionMetadata collection = versionedContentRepoService.getCollection(collectionVersion);
    RepoObjectMetadata found = null;
    for (RepoObjectMetadata objectMetadata : collection.getObjects()) {
      if (objectMetadata.getVersion().getKey().equals(objectKey)) {
        if (found != null) {
          throw new IllegalArgumentException("Multiple objects have key: " + objectKey);
        }
        found = objectMetadata;
      }
    }
    return found;
  }

  public InputStream readFileVersion(ArticleIdentity articleIdentity, UUID articleUuid, String fileKey) {
    RepoObjectMetadata objectMetadata = findObjectInCollection(RepoVersion.create(articleIdentity.getIdentifier(), articleUuid), fileKey);
    if (objectMetadata == null) throw new RestClientException("File not found", HttpStatus.NOT_FOUND);
    return contentRepoService.getRepoObject(objectMetadata.getVersion());
  }


  private static class RevisionVersionMapping {
    private final int versionNumber;
    private final Collection<Integer> revisionNumbers;

    public RevisionVersionMapping(int versionNumber) {
      Preconditions.checkArgument(versionNumber >= 0);
      this.versionNumber = versionNumber;
      this.revisionNumbers = new TreeSet<>();
    }
  }

  private static final Ordering<RevisionVersionMapping> ORDER_BY_VERSION_NUMBER = Ordering.natural().onResultOf(new Function<RevisionVersionMapping, Integer>() {
    @Override
    public Integer apply(RevisionVersionMapping input) {
      return input.versionNumber;
    }
  });

  /**
   * Describe the full list of back-end versions for one article, and the article revisions (if any) associated with
   * each version.
   *
   * @param articleIdentity
   * @return
   */
  public Transceiver listRevisions(final ArticleIdentity articleIdentity) {
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        return fetchRevisions(articleIdentity);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  private Collection<?> fetchRevisions(ArticleIdentity articleIdentity) throws IOException {
    List<RepoCollectionMetadata> versions = versionedContentRepoService.getCollectionVersions(articleIdentity.getIdentifier());
    Map<UUID, RevisionVersionMapping> mappings = Maps.newHashMapWithExpectedSize(versions.size());
    for (RepoCollectionMetadata version : versions) {
      RevisionVersionMapping mapping = new RevisionVersionMapping(version.getVersionNumber().getNumber());
      mappings.put(version.getVersion().getUuid(), mapping);
    }

    List<ArticleRevision> revisions = hibernateTemplate.find("from ArticleRevision where doi=?", articleIdentity.getIdentifier());
    for (ArticleRevision revision : revisions) {
      RevisionVersionMapping mapping = mappings.get(UUID.fromString(revision.getCrepoUuid()));
      mapping.revisionNumbers.add(revision.getRevisionNumber());
    }

    return ORDER_BY_VERSION_NUMBER.immutableSortedCopy(mappings.values());
  }

}
