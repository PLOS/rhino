/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.content.xml;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.ambraproject.rhino.model.ingest.AssetType;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents the manifest of an article .zip archive.
 */
public class ManifestXml extends AbstractXpathReader {

  /**
   * Indicates that the manifest contains invalid data.
   */
  public static class ManifestDataException extends RuntimeException {
    private ManifestDataException(String message) {
      super(message);
    }
  }

  // Like Maps.uniqueIndex, but in case of key collision, throws ManifestDataException with a helpful message
  private static <T, K> ImmutableMap<K, T> mapByUniqueKeys(Collection<? extends T> values,
                                                           Function<T, K> keyFunction,
                                                           Function<K, String> parentDescription) {
    Map<K, T> map = Maps.newLinkedHashMapWithExpectedSize(values.size());
    for (T value : values) {
      K key = Objects.requireNonNull(keyFunction.apply(Objects.requireNonNull(value)));
      T previous = map.put(key, value);
      if (previous != null) {
        throw new ManifestDataException(parentDescription.apply(key));
      }
    }
    return ImmutableMap.copyOf(map);
  }


  /**
   * Constructor.
   *
   * @param xml the XML content of the manifest file
   */
  public ManifestXml(Node xml) {
    super(xml);
  }


  private String requireAttribute(String attributeName, Node node) {
    String value = readString("@" + attributeName, node);
    if (value == null) {
      throw new ManifestDataException(String.format(
          "'%s' node must have '%s' attribute", node.getNodeName(), attributeName));
    }
    return value;
  }

  private class Parsed {
    private final ImmutableMap<String, Asset> assets;
    private final ImmutableList<ManifestFile> ancillaryFiles;

    private Parsed() {
      List<Asset> assets = new ArrayList<>();

      assets.add(parseAssetNode(AssetTagName.ARTICLE, readNode("/manifest/articleBundle/article")));
      for (Node objectNode : readNodeList("/manifest/articleBundle/object")) {
        assets.add(parseAssetNode(AssetTagName.OBJECT, objectNode));
      }

      this.assets = mapByUniqueKeys(assets, Asset::getUri,
          assetUri -> "Manifest has assets with duplicate uri: " + assetUri
      );

      this.ancillaryFiles = parseAncillaryFiles(readNode("/manifest/ancillary"));
    }

    private Asset parseAssetNode(AssetTagName assetTagName, Node assetNode) {
      String type = readString("@type", assetNode);
      String uri = requireAttribute("uri", assetNode);
      String strkImage = readString("@strkImage", assetNode);
      boolean isStrikingImage = Boolean.toString(true).equalsIgnoreCase(strkImage);

      List<Representation> representations = parseRepresentations(assetNode);
      return new Asset(assetTagName, type, uri, isStrikingImage, representations);
    }

    private ImmutableList<Representation> parseRepresentations(Node assetNode) {
      if (assetNode == null) return ImmutableList.of();
      List<Node> representationNodes = readNodeList("child::representation", assetNode);
      List<Representation> representations = new ArrayList<>(representationNodes.size());
      for (Node representationNode : representationNodes) {
        ManifestFile file = parseFile(representationNode);
        String type = requireAttribute("type", representationNode);
        representations.add(new Representation(file, type));
      }
      return ImmutableList.copyOf(representations);
    }

    private ImmutableList<ManifestFile> parseAncillaryFiles(Node ancillaryNode) {
      return (ancillaryNode == null) ? ImmutableList.of() : ImmutableList.copyOf(
          readNodeList("child::representation", ancillaryNode).stream()
              .map(this::parseFile)
              .collect(Collectors.toList()));
    }

    private ManifestFile parseFile(Node node) {
      String entry = requireAttribute("entry", node);
      String key = readString("@key", node);
      String mimetype = requireAttribute("mimetype", node);
      return new ManifestFile(entry, key, mimetype);
    }
  }

  private transient Parsed parsed;

  private Parsed getParsedObject() {
    return (this.parsed != null) ? this.parsed : (this.parsed = new Parsed());
  }

  public ImmutableList<Asset> getAssets() {
    return getParsedObject().assets.values().asList();
  }

  public Asset getArticleAsset() {
    return getAssets().stream()
        .filter(asset -> asset.getAssetTagName().equals(AssetTagName.ARTICLE))
        .findAny().orElseThrow(() ->
            new ManifestDataException("Manifest does not have <article> element"));
  }

  public ImmutableList<ManifestFile> getAncillaryFiles() {
    Parsed parsed = getParsedObject();
    return parsed.ancillaryFiles;
  }


  public static enum AssetTagName {
    ARTICLE, OBJECT, ANCILLARY;
  }

  public static class Asset {
    private final AssetTagName assetTagName;
    private final AssetType type;
    private final String uri;
    private final boolean isStrikingImage;
    private final ImmutableMap<String, Representation> representations;

    private Asset(AssetTagName assetTagName, String type, String uri, boolean isStrikingImage, List<Representation> representations) {
      this.assetTagName = Objects.requireNonNull(assetTagName);
      this.type = determineType(assetTagName, type);
      this.uri = Objects.requireNonNull(uri);
      this.isStrikingImage = isStrikingImage;
      this.representations = mapByUniqueKeys(representations, Representation::getType,
          representationType -> String.format("<%s type=\"%s\" uri=\"%s\"> has representations with duplicate type: %s",
              assetTagName, type, uri, representationType));
    }

    private static AssetType determineType(AssetTagName assetTagName, String type) {
      if (assetTagName == AssetTagName.ARTICLE) {
        if (type != null) {
          throw new ManifestDataException("<article> element should not have 'type' attribute");
        }
        return AssetType.ARTICLE;
      }
      if (type == null) {
        throw new ManifestDataException(String.format("'%s' node must have 'type' attribute", assetTagName));
      }
      return AssetType.fromIdentifier(type).orElseThrow(() ->
          new ManifestDataException("Unrecognized asset type: " + type));
    }

    public AssetTagName getAssetTagName() {
      return assetTagName;
    }

    public AssetType getType() {
      return type;
    }

    public String getUri() {
      return uri;
    }

    public boolean isStrikingImage() {
      return isStrikingImage;
    }

    public Optional<Representation> getRepresentation(String type) {
      return Optional.ofNullable(representations.get(type));
    }

    public ImmutableList<Representation> getRepresentations() {
      return representations.values().asList();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Asset asset = (Asset) o;
      return isStrikingImage == asset.isStrikingImage && assetTagName == asset.assetTagName && uri.equals(asset.uri)
          && representations.equals(asset.representations);
    }

    @Override
    public int hashCode() {
      return 31 * (31 * (31 * assetTagName.hashCode() + uri.hashCode())
          + Boolean.hashCode(isStrikingImage)) + representations.hashCode();
    }
  }

  public static class ManifestFile {
    private final String entry;
    private final Optional<String> key;
    private final String mimetype;

    private ManifestFile(String entry, String key, String mimetype) {
      this.entry = Objects.requireNonNull(entry);
      this.key = Optional.ofNullable(key);
      this.mimetype = Objects.requireNonNull(mimetype);
    }

    public String getEntry() {
      return entry;
    }

    public String getCrepoKey() {
      return key.orElse(entry);
    }

    public String getMimetype() {
      return mimetype;
    }

    @Override
    public boolean equals(Object o) {
      return this == o || o != null && getClass() == o.getClass()
          && entry.equals(((ManifestFile) o).entry)
          && key.equals(((ManifestFile) o).key)
          && mimetype.equals(((ManifestFile) o).mimetype);
    }

    @Override
    public int hashCode() {
      return 31 * (31 * entry.hashCode() + key.hashCode()) + mimetype.hashCode();
    }
  }

  public static class Representation {
    private final ManifestFile file;
    private final String type;

    private Representation(ManifestFile file, String type) {
      this.file = Objects.requireNonNull(file);
      this.type = Objects.requireNonNull(type);
    }

    public ManifestFile getFile() {
      return file;
    }

    public String getType() {
      return type;
    }

    @Override
    public boolean equals(Object o) {
      return this == o || o != null && getClass() == o.getClass()
          && file.equals(((Representation) o).file)
          && type.equals(((Representation) o).type);
    }

    @Override
    public int hashCode() {
      return 31 * file.hashCode() + type.hashCode();
    }
  }

}
