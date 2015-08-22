package org.ambraproject.rhino.service.impl;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.content.xml.AssetNodesByDoi;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.view.internal.RepoVersionRepr;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoVersion;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class AssetTable<T> {

  private static class Key {
    private final AssetIdentity id;
    private final FileType fileType;

    private Key(AssetIdentity id, FileType fileType) {
      this.id = Preconditions.checkNotNull(id);
      this.fileType = Preconditions.checkNotNull(fileType);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      return fileType.equals(((Key) o).fileType) && id.equals(((Key) o).id);
    }

    @Override
    public int hashCode() {
      return 31 * id.hashCode() + fileType.hashCode();
    }
  }

  private static class Value<T> {
    private final AssetType assetType;
    private final T fileLocator;

    private Value(AssetType assetType, T fileLocator) {
      this.assetType = Preconditions.checkNotNull(assetType);
      this.fileLocator = Preconditions.checkNotNull(fileLocator);
    }
  }

  private final ImmutableMap<Key, Value<T>> map;

  private AssetTable(Map<Key, Value<T>> map) {
    this.map = ImmutableMap.copyOf(map);
  }


  public ImmutableSet<AssetIdentity> getAssetIdentities() {
    ImmutableSet.Builder<AssetIdentity> set = ImmutableSet.builder();
    for (Key key : map.keySet()) {
      set.add(key.id);
    }
    return set.build();
  }

  public static interface Asset<T> {
    public AssetIdentity getIdentity();

    public AssetType getAssetType();

    public String getFileType();

    public T getFileLocator();
  }

  /**
   * Iterate over assets in the table.
   */
  // Implementation note: This is basically the same as exposing map.entrySet(), but with a convenient interface
  // obscuring our Key and Value classes.
  public Collection<Asset<T>> getAssets() {
    return Collections2.transform(map.entrySet(), new Function<Map.Entry<Key, Value<T>>, Asset<T>>() {
      @Override
      public Asset<T> apply(final Map.Entry<Key, Value<T>> mapEntry) {
        return new Asset<T>() {
          @Override
          public AssetIdentity getIdentity() {
            return mapEntry.getKey().id;
          }

          @Override
          public AssetType getAssetType() {
            return mapEntry.getValue().assetType;
          }

          @Override
          public String getFileType() {
            return mapEntry.getKey().fileType.identifier;
          }

          @Override
          public T getFileLocator() {
            return mapEntry.getValue().fileLocator;
          }
        };
      }
    });
  }


  private static RuntimeException unmatchedReprException(String reprName) {
    String message = "Manifest contains unmatched \"repr\" value: " + reprName;
    return new RestClientException(message, HttpStatus.BAD_REQUEST);
  }

  /**
   * The type of a file that represents an asset. Each asset type has a set of these that it uses, as defined by {@link
   * AssetType#getSupportedFileTypes}. File types may be shared among more than one asset type.
   */
  private static enum FileType {

    // Root-level files that belong to the article itself
    MANUSCRIPT, PRINTABLE,

    // The source representation of an image
    ORIGINAL,

    // The single display format for an inline graphic or similar asset that doesn't have different thumbnail sizes
    THUMBNAIL,

    // Display formats at different sizes for figures and tables
    SMALL, MEDIUM, INLINE, LARGE,

    // A supplementary information file, which should always be the only file with its DOI
    SUPPLEMENTARY,

    // An image that is used only as a striking image and is not otherwise part of the article
    STRIKING_IMAGE;

    private final String identifier = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());

    private static final ImmutableMap<String, FileType> BY_IDENTIFIER = Maps.uniqueIndex(EnumSet.allOf(FileType.class),
        new Function<FileType, String>() {
          @Override
          public String apply(FileType input) {
            return input.identifier;
          }
        });

    private static String unrecognizedFileType(String fileTypeIdentifier) {
      return String.format("File type not recognized: \"%s\". Must be one of: %s",
          fileTypeIdentifier, FileType.BY_IDENTIFIER.keySet());
    }
  }

  /**
   * The type of an asset (with a unique asset DOI, represented by one or more files) as determined by where the asset
   * DOI is referenced in the manuscript of its parent article.
   */
  private static enum AssetType {
    ARTICLE {
      private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.MANUSCRIPT, FileType.PRINTABLE);
      private final ImmutableSet<FileType> REQUIRED = Sets.immutableEnumSet(FileType.MANUSCRIPT);

      @Override
      protected ImmutableSet<FileType> getSupportedFileTypes() {
        return TYPES;
      }

      @Override
      protected ImmutableSet<FileType> getRequiredFileTypes() {
        return REQUIRED;
      }

      @Override
      protected FileType getFileType(String reprName) {
        switch (reprName) {
          case "XML":
            return FileType.MANUSCRIPT;
          case "PDF":
            return FileType.PRINTABLE;
          default:
            throw unmatchedReprException(reprName);
        }
      }
    },

    FIGURE {
      @Override
      protected ImmutableSet<FileType> getSupportedFileTypes() {
        return STANDARD_THUMBNAIL_FILE_TYPES;
      }

      @Override
      protected FileType getFileType(String reprName) {
        return getFileTypeForStandardThumbnails(reprName);
      }
    },

    TABLE {
      @Override
      protected ImmutableSet<FileType> getSupportedFileTypes() {
        return STANDARD_THUMBNAIL_FILE_TYPES;
      }

      @Override
      protected FileType getFileType(String reprName) {
        return getFileTypeForStandardThumbnails(reprName);
      }
    },

    GRAPHIC {
      private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.ORIGINAL, FileType.THUMBNAIL);

      @Override
      protected ImmutableSet<FileType> getSupportedFileTypes() {
        return TYPES;
      }

      @Override
      protected FileType getFileType(String reprName) {
        switch (reprName) {
          case "TIF":
          case "GIF":
            return FileType.ORIGINAL;
          case "PNG":
            return FileType.THUMBNAIL;
          default:
            throw unmatchedReprException(reprName);
        }
      }
    },

    SUPPLEMENTARY_MATERIAL {
      private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.SUPPLEMENTARY);

      @Override
      protected ImmutableSet<FileType> getSupportedFileTypes() {
        return TYPES;
      }

      @Override
      protected FileType getFileType(String reprName) {
        // Accept all file types
        return FileType.SUPPLEMENTARY;
      }
    },

    STANDALONE_STRIKING_IMAGE {
      private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.STRIKING_IMAGE);

      @Override
      protected ImmutableSet<FileType> getSupportedFileTypes() {
        return TYPES;
      }

      @Override
      protected FileType getFileType(String reprName) {
        // Accept all file types
        // TODO: Validate on expected image file types?
        return FileType.STRIKING_IMAGE;
      }
    };

    // Shared by FIGURE and TABLE
    private static final ImmutableSet<FileType> STANDARD_THUMBNAIL_FILE_TYPES = Sets.immutableEnumSet(
        FileType.ORIGINAL, FileType.SMALL, FileType.INLINE, FileType.MEDIUM, FileType.LARGE);

    private static FileType getFileTypeForStandardThumbnails(String reprName) {
      switch (reprName) {
        case "TIF":
        case "TIFF":
          return FileType.ORIGINAL;
        case "PNG_S":
          return FileType.SMALL;
        case "PNG_I":
          return FileType.INLINE;
        case "PNG_M":
          return FileType.MEDIUM;
        case "PNG_L":
          return FileType.LARGE;
        default:
          throw unmatchedReprException(reprName);
      }
    }

    private final String identifier = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());

    /**
     * Match a repr name (as used in {@link org.ambraproject.rhino.content.xml.ManifestXml}) to a file type as defined
     * for this asset type. Must be one of the values contained in the set that this asset type returns for {@link
     * #getSupportedFileTypes}.
     *
     * @param reprName an uppercase repr name from a manifest file
     * @return the file type that will represent the file in JSON article metadata
     * @throws RestClientException if {@code reprName} is not matched to a file type for this asset type
     */
    protected abstract FileType getFileType(String reprName);

    /**
     * Return the set of file types that this asset type may return from {@link #getFileType}.
     */
    protected abstract ImmutableSet<FileType> getSupportedFileTypes();

    /**
     * Return the set of file types that an ingestible must supply for each asset of this type. Must be a subset of
     * {@link #getSupportedFileTypes}.
     */
    protected ImmutableSet<FileType> getRequiredFileTypes() {
      return getSupportedFileTypes();
    }

    private static final ImmutableMap<String, AssetType> BY_IDENTIFIER = Maps.uniqueIndex(EnumSet.allOf(AssetType.class),
        new Function<AssetType, String>() {
          @Override
          public String apply(AssetType input) {
            return input.identifier;
          }
        });
  }


  /**
   * Build an asset table from input being ingested.
   * <p/>
   * The asset table can then be persisted by {@link #buildAsAssetMetadata}.
   *
   * @param assetNodeMap encapsulated descriptions of references to asset DOIs in the manuscript
   * @param manifest     encapsulated manifest document describing the supplied files
   * @return the built asset table
   */
  public static AssetTable<String> buildFromIngestible(AssetNodesByDoi assetNodeMap, ManifestXml manifest) {
    Map<Key, Value<String>> ingestibleEntryNames = new LinkedHashMap<>();
    for (ManifestXml.Asset asset : manifest.parse()) {
      AssetType assetType = findAssetType(assetNodeMap, asset);
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        insertEntryForAssetRepr(ingestibleEntryNames, asset, assetType, representation);
      }
    }
    AssetTable<String> created = new AssetTable<>(ingestibleEntryNames);
    created.validateFileTypes();
    return created;
  }

  /**
   * Apply validation according to {@link AssetType#getRequiredFileTypes}.
   *
   * @throws RestClientException if an asset has less than the required set of file types
   */
  private void validateFileTypes() {
    Map<AssetIdentity, AssetType> assetTypes = new HashMap<>();
    SetMultimap<AssetIdentity, FileType> fileTypes = HashMultimap.create();

    // First pass to populate the two maps, grouping asset files by the asset they belong to
    for (Map.Entry<Key, Value<T>> entry : map.entrySet()) {
      AssetIdentity assetIdentity = entry.getKey().id;
      AssetType assetType = entry.getValue().assetType;
      FileType fileType = entry.getKey().fileType;

      AssetType previous = assetTypes.put(assetIdentity, assetType);
      if (previous != null && !previous.equals(assetType)) {
        // Should be impossible except as a bug. Indicate a server error.
        String message = String.format("Inconsistent asset types for %s: %s, %s", assetIdentity, previous, assetType);
        throw new IllegalStateException(message);
      }

      fileTypes.put(assetIdentity, fileType);
    }

    // Second pass to validate each set of files against its asset type
    for (Map.Entry<AssetIdentity, Set<FileType>> entry : Multimaps.asMap(fileTypes).entrySet()) {
      AssetIdentity assetIdentity = entry.getKey();
      AssetType assetType = Preconditions.checkNotNull(assetTypes.get(assetIdentity));
      Set<FileType> assetFileTypes = entry.getValue();

      Set<FileType> requiredFileTypes = assetType.getRequiredFileTypes();
      if (!assetFileTypes.containsAll(requiredFileTypes)) {
        // May be caused by an incomplete ingestible. Indicate a client error.
        String message = String.format("Asset \"%s\" is of type: %s. Received files: %s. Missing files: %s.",
            assetIdentity, assetType, assetFileTypes, Sets.difference(requiredFileTypes, assetFileTypes));
        throw new RestClientException(message, HttpStatus.BAD_REQUEST);
      }
    }
  }

  private static void insertEntryForAssetRepr(Map<Key, Value<String>> ingestibleEntryNames,
                                              ManifestXml.Asset asset,
                                              AssetType assetType,
                                              ManifestXml.Representation representation) {
    String entryName = representation.getEntry();
    FileType fileType = assetType.getFileType(representation.getName());

    Key key = new Key(AssetIdentity.create(asset.getUri()), fileType);
    Value<String> value = new Value<>(assetType, entryName);

    Value<String> previous = ingestibleEntryNames.put(key, value);
    if (previous != null) {
      String message = String.format("More than one file has uri=\"%s\" and type=\"%s\". " +
              "(Entry names: \"%s\"; \"%s\")",
          asset.getUri(), fileType, entryName, previous.fileLocator);
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
  }

  private static AssetType getByXmlNodeName(String nodeName) {
    switch (nodeName) {
      case "fig":
        return AssetType.FIGURE;
      case "table-wrap":
        return AssetType.TABLE;
      case "graphic":
      case "disp-formula":
      case "inline-formula":
        return AssetType.GRAPHIC;
      case "supplementary-material":
        return AssetType.SUPPLEMENTARY_MATERIAL;
      default:
        throw new RestClientException("XML node name could not be matched to asset type: " + nodeName, HttpStatus.BAD_REQUEST);
    }
  }

  private static AssetType findAssetType(AssetNodesByDoi assetNodeMap, ManifestXml.Asset asset) {
    if (asset.getAssetType().equals(ManifestXml.AssetType.ARTICLE)) {
      return AssetType.ARTICLE;
    }
    AssetIdentity assetIdentity = AssetIdentity.create(asset.getUri());
    if (!assetNodeMap.getDois().contains(assetIdentity.getIdentifier())) {
      if (asset.isStrikingImage()) {
        return AssetType.STANDALONE_STRIKING_IMAGE;
      } else {
        throw new RestClientException("Asset not mentioned in manuscript", HttpStatus.BAD_REQUEST);
      }
    }

    List<Node> nodes = assetNodeMap.getNodes(assetIdentity.getIdentifier());
    AssetType identifiedType = null;
    for (Node node : nodes) {
      String nodeName = node.getNodeName();
      AssetType assetType = getByXmlNodeName(nodeName);
      if (assetType != null) {
        if (identifiedType == null) {
          identifiedType = assetType;
        } else if (!identifiedType.equals(assetType)) {
          String message = String.format("Ambiguous nodes: %s, %s", identifiedType, assetType);
          throw new RestClientException(message, HttpStatus.BAD_REQUEST);
        }
      }
    }
    if (identifiedType == null) {
      throw new RestClientException("Type not recognized", HttpStatus.BAD_REQUEST);
    }
    return identifiedType;
  }

  // TODO: Refactor to group asset files by identifier, and store only one asset type for the group?
  private static AssetType getCommonAssetType(Iterable<? extends Asset<?>> assets) {
    Iterator<? extends Asset<?>> iterator = assets.iterator();
    if (!iterator.hasNext()) {
      throw new IllegalArgumentException("No assets");
    }

    Asset<?> asset = iterator.next();
    AssetType assetType = asset.getAssetType();
    AssetIdentity identity = asset.getIdentity();

    while (iterator.hasNext()) {
      Asset<?> nextAsset = iterator.next();
      AssetIdentity nextIdentity = nextAsset.getIdentity();
      if (!identity.equals(nextIdentity)) {
        String message = String.format("Inconsistent asset types: \"%s\", \"%s\"", identity, nextIdentity);
        throw new IllegalArgumentException(message);
      }

      AssetType nextAssetType = nextAsset.getAssetType();
      if (!assetType.equals(nextAssetType)) {
        String message = String.format("Inconsistent asset types (%s, %s) for: %s", assetType, nextAssetType, identity);
        throw new IllegalArgumentException(message);
      }
    }

    return assetType;
  }

  /**
   * Build an asset metadata table. The table goes into the JSON metadata of a content repo collection representing an
   * ingested article. One of these tables, inside a repo collection, can be parsed back into an {@code AssetTable}
   * using {@link #buildFromAssetMetadata}.
   * <p/>
   * This can be called only after the assets in this table have been persisted to the content repo. The argument is a
   * map from the assets' file locator values (see {@link org.ambraproject.rhino.service.impl.AssetTable.Asset#getFileLocator})
   * to the keys of the persistent objects.
   *
   * @param repoObjectVersions a map from file locators to persisted objects
   * @return an asset metadata table suitable for serializing to JSON
   */
  public Map<String, Object> buildAsAssetMetadata(Map<? super T, RepoVersion> repoObjectVersions) {
    Collection<Asset<T>> assets = getAssets();
    ListMultimap<String, Asset<T>> filesByAsset = LinkedListMultimap.create();
    for (Asset<T> asset : assets) {
      filesByAsset.put(asset.getIdentity().getIdentifier(), asset);
    }

    /*
     * TODO: Provide assets as an ordered list?
     * Once this is in JSON, the order of assets will be lost. (LinkedHashMap is only for ease of human readability.)
     * Even if we change to a list, the order from getAssets() comes from the manifest. It might be more correct (for
     * display purposes?) to use the same order in which the DOIs are referenced in the manuscript.
     */
    Map<String, Object> assetMetadataTable = new LinkedHashMap<>();
    for (Map.Entry<String, List<Asset<T>>> entry : Multimaps.asMap(filesByAsset).entrySet()) {
      List<Asset<T>> entryAssets = entry.getValue();
      Map<String, Object> assetMetadata = new LinkedHashMap<>();

      assetMetadata.put("type", getCommonAssetType(entryAssets).identifier);

      Map<String, Object> fileTable = new LinkedHashMap<>();
      for (Asset<T> asset : entryAssets) {
        RepoVersion repoVersion = repoObjectVersions.get(asset.getFileLocator());
        fileTable.put(asset.getFileType(), new RepoVersionRepr(repoVersion));
      }
      assetMetadata.put("files", fileTable);

      assetMetadataTable.put(entry.getKey(), assetMetadata);
    }
    return assetMetadataTable;
  }

  /**
   * Reconstruct the asset table from a persisted article.
   *
   * @param collection a collection representing an article
   * @return the article's asset table
   */
  public static AssetTable<RepoVersion> buildFromAssetMetadata(RepoCollectionMetadata collection) {
    Map<Key, Value<RepoVersion>> map = new LinkedHashMap<>();
    Map<String, Map<String, ?>> assets = (Map<String, Map<String, ?>>) ((Map) collection.getJsonUserMetadata().get()).get("assets");
    for (Map.Entry<String, Map<String, ?>> assetEntry : assets.entrySet()) {
      AssetIdentity id = AssetIdentity.create(assetEntry.getKey());
      Map<String, ?> asset = assetEntry.getValue();

      String type = (String) asset.get("type");
      AssetType assetType = AssetType.BY_IDENTIFIER.get(type);
      if (assetType == null) throw new RuntimeException("Unrecognized type: " + type);

      Map<String, ?> files = (Map<String, ?>) asset.get("files");
      for (Map.Entry<String, ?> fileEntry : files.entrySet()) {
        String fileType = fileEntry.getKey();
        RepoVersion repoVersion = RepoVersionRepr.read((Map<?, ?>) fileEntry.getValue());
        FileType fileTypeObj = FileType.BY_IDENTIFIER.get(fileType);
        if (fileTypeObj == null) {
          throw new IllegalArgumentException(FileType.unrecognizedFileType(fileType));
        }
        map.put(new Key(id, fileTypeObj), new Value<>(assetType, repoVersion));
      }
    }
    return new AssetTable<>(map);
  }

  /**
   * Look up the file locator for one file representing an asset.
   *
   * @param id       the asset's identifier (a DOI, not representing an individual file)
   * @param fileType a string indicating one of the files that belong to the asset
   * @return the file locator for the asset
   */
  public T lookup(AssetIdentity id, String fileType) {
    FileType fileTypeObj = FileType.BY_IDENTIFIER.get(fileType);
    if (fileTypeObj == null) {
      throw new RestClientException(FileType.unrecognizedFileType(fileType), HttpStatus.NOT_FOUND);
    }
    Key key = new Key(id, fileTypeObj);
    Value<T> value = map.get(key);
    if (value == null) {
      String message = String.format("No asset found with id=\"%s\" and fileType=\"%s\"", id, fileType);
      throw new IllegalArgumentException(message);
    }
    return value.fileLocator;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return map.equals(((AssetTable) o).map);
  }

  @Override
  public int hashCode() {
    return map.hashCode();
  }
}
