package org.ambraproject.rhino.service.impl;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class AssetTable<T> {

  private static class Key {
    private final AssetIdentity id;
    private final String fileType;

    private Key(AssetIdentity id, String fileType) {
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
            return mapEntry.getKey().fileType;
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

  private static enum AssetType {
    ARTICLE {
      protected String getFileType(String reprName) {
        switch (reprName) {
          case "XML":
            return "manuscript";
          case "PDF":
            return "printable";
          default:
            throw unmatchedReprException(reprName);
        }
      }
    },
    FIGURE {
      @Override
      protected String getFileType(String reprName) {
        return getFileTypeForStandardThumbnails(reprName);
      }
    },
    TABLE {
      @Override
      protected String getFileType(String reprName) {
        return getFileTypeForStandardThumbnails(reprName);
      }
    },
    GRAPHIC {
      @Override
      protected String getFileType(String reprName) {
        switch (reprName) {
          case "TIF":
            return "original";
          case "PNG":
            return "thumbnail";
          default:
            throw unmatchedReprException(reprName);
        }
      }
    },
    SUPPLEMENTARY_MATERIAL {
      @Override
      protected String getFileType(String reprName) {
        // Accept all file types
        return "supplementary";
      }
    },
    STANDALONE_STRIKING_IMAGE {
      @Override
      protected String getFileType(String reprName) {
        // Accept all file types
        // TODO: Validate on expected image file types?
        return "strikingImage";
      }
    };

    private static String getFileTypeForStandardThumbnails(String reprName) {
      switch (reprName) {
        case "TIF":
        case "TIFF":
          return "original";
        case "PNG_S":
          return "small";
        case "PNG_I":
          return "inline";
        case "PNG_M":
          return "medium";
        case "PNG_L":
          return "large";
        default:
          throw unmatchedReprException(reprName);
      }
    }

    private final String identifier = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());

    protected abstract String getFileType(String reprName);

    private static final ImmutableMap<String, AssetType> BY_IDENTIFIER = Maps.uniqueIndex(EnumSet.allOf(AssetType.class),
        new Function<AssetType, String>() {
          @Override
          public String apply(AssetType input) {
            return input.identifier;
          }
        });
  }


  public static AssetTable<String> buildFromIngestible(AssetNodesByDoi assetNodeMap, ManifestXml manifest) {
    Map<Key, Value<String>> ingestibleEntryNames = new LinkedHashMap<>();
    for (ManifestXml.Asset asset : manifest.parse()) {
      AssetType assetType = findAssetType(assetNodeMap, asset);
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        insertEntryForAssetRepr(ingestibleEntryNames, asset, assetType, representation);
      }
    }
    return new AssetTable<>(ingestibleEntryNames);
  }

  private static void insertEntryForAssetRepr(Map<Key, Value<String>> ingestibleEntryNames,
                                              ManifestXml.Asset asset,
                                              AssetType assetType,
                                              ManifestXml.Representation representation) {
    String entryName = representation.getEntry();
    String fileType = assetType.getFileType(representation.getName());

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
        map.put(new Key(id, fileType), new Value<>(assetType, repoVersion));
      }
    }
    return new AssetTable<>(map);
  }

  public T lookup(AssetIdentity id, String fileType) {
    Key key = new Key(id, fileType);
    Value<T> value = map.get(key);
    if (value == null) throw new IllegalArgumentException();
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
