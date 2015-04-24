package org.ambraproject.rhino.service.impl;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.AssetNodesByDoi;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.view.internal.RepoVersionRepr;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoVersion;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
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
    private final T fileLocator;
    private final String reprName;

    private Value(T fileLocator, String reprName) {
      this.fileLocator = Preconditions.checkNotNull(fileLocator);
      this.reprName = Preconditions.checkNotNull(reprName);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      return fileLocator.equals(((Value) o).fileLocator) && reprName.equals(((Value) o).reprName);
    }

    @Override
    public int hashCode() {
      return 31 * fileLocator.hashCode() + reprName.hashCode();
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

    public String getFileType();

    public T getFileLocator();

    public String getReprName();
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
          public String getFileType() {
            return mapEntry.getKey().fileType;
          }

          @Override
          public T getFileLocator() {
            return mapEntry.getValue().fileLocator;
          }

          @Override
          public String getReprName() {
            return mapEntry.getValue().reprName;
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
        switch (reprName) {
          case "TIF":
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
    SUPP_INFO {
      @Override
      protected String getFileType(String reprName) {
        return "supplemental";
      }
    };

    protected abstract String getFileType(String reprName);
  }


  public static AssetTable<String> buildFromIngestible(AssetNodesByDoi assetNodeMap, ManifestXml manifest) {
    Map<Key, Value<String>> ingestibleEntryNames = new LinkedHashMap<>();
    for (ManifestXml.Asset asset : manifest.parse()) {
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        AssetIdentity assetIdentity = AssetIdentity.create(asset.getUri());
        String entryName = representation.getEntry();

        AssetType assetType = asset.getAssetType().equals(ManifestXml.AssetType.ARTICLE) ? AssetType.ARTICLE
            : getAssetType(assetNodeMap, assetIdentity);
        String fileType = assetType.getFileType(representation.getName());

        Key key = new Key(assetIdentity, fileType);
        Value<String> value = new Value<>(entryName, representation.getName());
        Value<String> previous = ingestibleEntryNames.put(key, value);
        if (previous != null) {
          String message = String.format("More than one file has uri=\"%s\" and repr=\"%s\"",
              asset.getUri(), representation.getName());
          throw new RestClientException(message, HttpStatus.BAD_REQUEST);
        }
      }
    }
    return new AssetTable<>(ingestibleEntryNames);
  }

  private static AssetType getByXmlNodeName(String nodeName) {
    switch (nodeName) {
      case "fig":
        return AssetType.FIGURE;
      case "disp-formula":
        return AssetType.GRAPHIC;
      case "supplementary-material":
        return AssetType.SUPP_INFO;
      default:
        throw new RestClientException("XML node name could not be matched to asset type: " + nodeName, HttpStatus.BAD_REQUEST);
    }
  }

  private static AssetType getAssetType(AssetNodesByDoi assetNodeMap, AssetIdentity assetId) {
    if (!assetNodeMap.getDois().contains(assetId.getIdentifier())) {
      throw new RestClientException("Asset not mentioned in manuscript", HttpStatus.BAD_REQUEST);
    }

    List<Node> nodes = assetNodeMap.getNodes(assetId.getIdentifier());
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

  public List<Map<String, Object>> buildAsAssetMetadata(Map<? super T, RepoVersion> repoObjectVersions) {
    Collection<Asset<T>> assets = getAssets();
    List<Map<String, Object>> assetMetadataList = new ArrayList<>(assets.size());
    for (Asset<T> asset : assets) {
      ImmutableMap.Builder<String, Object> assetMetadata = ImmutableMap.builder();
      assetMetadata.put("id", asset.getIdentity().getIdentifier());
      assetMetadata.put("repr", asset.getReprName());
      RepoVersion repoVersion = repoObjectVersions.get(asset.getFileLocator());
      assetMetadata.put("object", new RepoVersionRepr(repoVersion));
      assetMetadataList.add(assetMetadata.build());
    }
    return assetMetadataList;
  }

  public static AssetTable<RepoVersion> buildFromAssetMetadata(RepoCollectionMetadata collection,
                                                               ArticleXml articleXml) {
    Map<Key, Value<RepoVersion>> map = new LinkedHashMap<>();
    List<Map<String, ?>> assets = (List<Map<String, ?>>) ((Map) collection.getJsonUserMetadata().get()).get("assets");
    for (Map<String, ?> asset : assets) {
      Map<String, String> object = (Map<String, String>) asset.get("object");
      RepoVersion repoVersion = RepoVersionRepr.read(object);

      AssetIdentity id = AssetIdentity.create((String) asset.get("id"));
      String reprName = (String) asset.get("repr");
      String fileType = getAssetType(articleXml.findAllAssetNodes(), id).getFileType(reprName);

      map.put(new Key(id, fileType), new Value<>(repoVersion, reprName));
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
