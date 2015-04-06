package org.ambraproject.rhino.service;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.model.ArticleAssociation;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.impl.AmbraService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.plos.crepo.model.RepoCollection;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArticleRevisionService extends AmbraService {

  @Autowired
  private ContentRepoService versionedContentRepoService;
  @Autowired
  private Gson crepoGson;

  public RepoCollectionMetadata ingest(InputStream archiveStream) throws IOException, XmlContentException {
    String prefix = "ingest_" + new Date().getTime() + "_";
    Map<String, File> extracted = new HashMap<>();

    try {
      File manifestFile = null;
      try (ZipInputStream zipStream = new ZipInputStream(archiveStream)) {
        ZipEntry entry;
        while ((entry = zipStream.getNextEntry()) != null) {
          File tempFile = File.createTempFile(prefix, null);
          try (OutputStream tempFileStream = new FileOutputStream(tempFile)) {
            ByteStreams.copy(zipStream, tempFileStream);
          }

          String name = entry.getName();
          extracted.put(name, tempFile);
          if (name.equalsIgnoreCase("manifest.xml")) {
            manifestFile = tempFile;
          }
        }
      } finally {
        archiveStream.close();
      }
      if (manifestFile == null) {
        // TODO complain
      }

      return writeCollection(extracted, manifestFile);
    } finally {
      for (File file : extracted.values()) {
        file.delete();
      }
    }
  }

  private RepoCollectionMetadata writeCollection(Map<String, File> files, File manifestFile) throws IOException, XmlContentException {
    ManifestXml manifestXml;
    try (InputStream manifestStream = new BufferedInputStream(new FileInputStream(manifestFile))) {
      manifestXml = new ManifestXml(parseXml(manifestStream));
    }
    ImmutableList<ManifestXml.Asset> assets = manifestXml.parse();

    ManifestXml.Asset manuscriptAsset = null;
    ManifestXml.Representation manuscriptRepr = null;
    for (ManifestXml.Asset asset : assets) {
      Optional<String> mainEntry = asset.getMainEntry();
      if (mainEntry.isPresent()) {
        for (ManifestXml.Representation representation : asset.getRepresentations()) {
          if (representation.getEntry().equals(mainEntry.get())) {
            manuscriptRepr = representation;
            break;
          }
        }
        manuscriptAsset = asset;
        break;
      }
    }
    if (manuscriptAsset == null || manuscriptRepr == null) {
      throw new IllegalArgumentException(); // TODO Better failure
    }

    File manuscriptFile = files.get(manuscriptRepr.getEntry());
    ArticleIdentity articleIdentity;
    Article articleMetadata;
    try (InputStream manuscriptStream = new BufferedInputStream(new FileInputStream(manuscriptFile))) {
      ArticleXml parsedArticle = new ArticleXml(parseXml(manuscriptStream));
      articleIdentity = parsedArticle.readDoi();
      articleMetadata = parsedArticle.build(new Article());
      articleMetadata.setDoi(articleIdentity.getKey()); // Should ArticleXml.build do this itself?
    }

    Map<String, RepoObject> toUpload = new LinkedHashMap<>(); // keys are zip entry names

    RepoObject manifestObject = new RepoObject.RepoObjectBuilder("manuscript/" + articleIdentity)
        .fileContent(manuscriptFile)
        .contentType(MediaType.APPLICATION_XML)
        .downloadName(articleIdentity.forXmlAsset().getFileName())
        .build();
    toUpload.put(manuscriptRepr.getEntry(), manifestObject);

    for (ManifestXml.Asset asset : assets) {
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        String entry = representation.getEntry();
        File file = files.get(entry);
        if (file.equals(manuscriptFile)) continue;
        String key = representation.getName() + "/" + AssetIdentity.create(asset.getUri());
        RepoObject repoObject = new RepoObject.RepoObjectBuilder(key)
            .fileContent(file)
                // TODO Add more metadata. Extract from articleMetadata and manifestXml as necessary.
            .build();
        toUpload.put(entry, repoObject);
      }
    }

    // Post files
    Map<String, RepoVersion> created = new LinkedHashMap<>();
    for (Map.Entry<String, RepoObject> entry : toUpload.entrySet()) { // Excellent candidate for parallelization! I can haz JDK8 plz?
      RepoObject repoObject = entry.getValue();
      RepoObjectMetadata createdMetadata = versionedContentRepoService.autoCreateRepoObject(repoObject);
      created.put(entry.getKey(), createdMetadata.getVersion());
    }

    ArticleUserMetadata userMetadataForCollection = buildArticleAsUserMetadata(manifestXml, created);

    // Create collection
    RepoCollection collection = RepoCollection.builder()
        .setKey(articleIdentity.toString())
        .setObjects(created.values())
        .setUserMetadata(crepoGson.toJson(userMetadataForCollection.map))
        .build();
    RepoCollectionMetadata collectionMetadata = versionedContentRepoService.autoCreateCollection(collection);

    // Associate DOIs
    for (String assetDoi : userMetadataForCollection.dois) {
      assetDoi = AssetIdentity.create(assetDoi).toString();
      ArticleAssociation existing = (ArticleAssociation) DataAccessUtils.uniqueResult(hibernateTemplate.find(
          "from ArticleAssociation where doi=?", assetDoi));
      if (existing == null) {
        ArticleAssociation association = new ArticleAssociation();
        association.setDoi(assetDoi);
        association.setParentArticleDoi(articleIdentity.toString());
        hibernateTemplate.persist(association);
      } else if (!existing.getParentArticleDoi().equalsIgnoreCase(articleIdentity.toString())) {
        throw new RuntimeException("Asset DOI already belongs to another parent article"); // TODO: Rollback
      } // else, leave it as is
    }

    return collectionMetadata;
  }

  private static class ArticleUserMetadata {
    private final Map<String, Object> map;
    private final Set<String> dois;

    private ArticleUserMetadata(Map<String, Object> map, Collection<String> dois) {
      this.map = ImmutableMap.copyOf(map);
      this.dois = ImmutableSet.copyOf(dois);
    }
  }

  private ArticleUserMetadata buildArticleAsUserMetadata(ManifestXml manifestXml, Map<String, RepoVersion> objects) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("format", "nlm");

    String manuscriptKey = manifestXml.getArticleXml();
    map.put("manuscript", new RepoVersionRepr(objects.get(manuscriptKey)));

    List<ManifestXml.Asset> assetSpec = manifestXml.parse();
    List<Map<String, Object>> assetList = new ArrayList<>(assetSpec.size());
    List<String> assetDois = new ArrayList<>(assetSpec.size());
    for (ManifestXml.Asset asset : assetSpec) {
      Map<String, Object> assetMetadata = new LinkedHashMap<>();
      String doi = AssetIdentity.create(asset.getUri()).toString();
      assetMetadata.put("doi", doi);
      assetDois.add(doi);

      Map<String, Object> assetObjects = new LinkedHashMap<>();
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        RepoVersion objectForRepr = objects.get(representation.getEntry());
        assetObjects.put(representation.getName(), new RepoVersionRepr(objectForRepr));
      }
      assetMetadata.put("objects", assetObjects);

      assetList.add(assetMetadata);
    }
    map.put("assets", assetList);

    return new ArticleUserMetadata(map, assetDois);
  }


  public Transceiver readVersion(ArticleIdentity articleIdentity, UUID uuid) {
    final RepoVersion version = RepoVersion.create(articleIdentity.toString(), uuid);
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
    RepoObjectMetadata objectMetadata = findObjectInCollection(RepoVersion.create(articleIdentity.toString(), articleUuid), fileKey);
    if (objectMetadata == null) throw new RestClientException("File not found", HttpStatus.NOT_FOUND);
    return contentRepoService.getRepoObject(objectMetadata.getVersion());
  }


  private static class RepoVersionRepr {
    private final String key;
    private final String uuid;

    private RepoVersionRepr(RepoVersion repoVersion) {
      this.key = repoVersion.getKey();
      this.uuid = repoVersion.getUuid().toString();
    }
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
    List<RepoCollectionMetadata> versions = versionedContentRepoService.getCollectionVersions(articleIdentity.toString());
    Map<UUID, RevisionVersionMapping> mappings = Maps.newHashMapWithExpectedSize(versions.size());
    for (RepoCollectionMetadata version : versions) {
      RevisionVersionMapping mapping = new RevisionVersionMapping(version.getVersionNumber().getNumber());
      mappings.put(version.getVersion().getUuid(), mapping);
    }

    List<ArticleRevision> revisions = hibernateTemplate.find("from ArticleRevision where doi=?", articleIdentity.toString());
    for (ArticleRevision revision : revisions) {
      RevisionVersionMapping mapping = mappings.get(UUID.fromString(revision.getCrepoUuid()));
      mapping.revisionNumbers.add(revision.getRevisionNumber());
    }

    return ORDER_BY_VERSION_NUMBER.immutableSortedCopy(mappings.values());
  }

}
