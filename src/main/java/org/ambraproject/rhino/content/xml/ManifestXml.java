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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the manifest of an article .zip archive.
 */
public class ManifestXml extends AbstractXpathReader {

  /**
   * Constructor.
   *
   * @param xml the XML content of the manifest file
   */
  public ManifestXml(Node xml) {
    super(xml);
  }

  /**
   * @return the name of the file in the zip archive that is the XML article
   */
  public String getArticleXml() {
    return readString("//article/@main-entry");
  }

  /**
   * @return the URI of the "striking image" associated with this article
   */
  public String getStrkImgURI() {
    return readString("//object[@strkImage='True']/@uri");
  }

  private transient ImmutableMap<String, String> uriMap;

  public String getUriForFile(String filename) {
    ImmutableMap<String, String> uriMap = (this.uriMap == null) ? (this.uriMap = buildUriMap()) : this.uriMap;
    return uriMap.get(filename);
  }

  private ImmutableMap<String, String> buildUriMap() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    List<Node> objectNodes = readNodeList("//article|//object");
    for (Node objectNode : objectNodes) {
      // TODO: Is there an efficient way to pull this logic into an XPath query?
      String uri = readString("@uri", objectNode);
      List<Node> representationNode = readNodeList("child::representation", objectNode);
      for (Node reprNode : representationNode) {
        String filename = readString("@entry", reprNode);
        builder.put(filename, uri);
      }
    }
    return builder.build();
  }


  private transient ImmutableList<Asset> parsedAssets;

  public ImmutableList<Asset> parse() {
    if (parsedAssets != null) return parsedAssets;

    // Build into ImmutableMap to force all Assets to have unique identities
    ImmutableMap.Builder<String, Asset> assets = ImmutableMap.builder();

    List<Node> assetNodes = readNodeList("//article|//object");
    for (Node assetNode : assetNodes) {
      String nodeName = assetNode.getNodeName();
      String uri = readString("@uri", assetNode);
      String mainEntry = readString("@main-entry", assetNode);
      String strkImage = readString("@strkImage", assetNode);

      AssetType assetType = AssetType.fromNodeName(nodeName);
      boolean isStrikingImage = Boolean.toString(true).equalsIgnoreCase(strkImage);

      List<Node> representationNodes = readNodeList("child::representation", assetNode);
      List<Representation> representations = new ArrayList<>(representationNodes.size());
      for (Node representationNode : representationNodes) {
        String name = readString("@name", representationNode);
        String entry = readString("@entry", representationNode);
        Representation representation = new Representation(name, entry);
        if (!isJunkXml(assetType, mainEntry, representation)) {
          representations.add(representation);
        }
      }

      assets.put(uri, new Asset(assetType, uri, mainEntry, isStrikingImage, representations));
    }

    return parsedAssets = assets.build().values().asList();
  }

  /**
   * As a special case, due to legacy reasons, the article asset may contain other versions of the XML file that we
   * don't want to save. (In PLOS ingestibles, these show up as {@code *.xml.orig} files.)
   * <p>
   * Suppress all XML files in the article asset except for the one that is designated as the main entry.
   */
  private static boolean isJunkXml(AssetType assetType, String mainEntry, Representation representation) {
    return (assetType == AssetType.ARTICLE) && representation.getName().equals("XML")
        && !Objects.equals(mainEntry, representation.getEntry());
  }

  public static enum AssetType {
    ARTICLE, OBJECT;

    private static AssetType fromNodeName(String nodeName) {
      switch (nodeName) {
        case "article":
          return ARTICLE;
        case "object":
          return OBJECT;
        default:
          throw new IllegalArgumentException();
      }
    }
  }

  public static class Asset {
    private final AssetType assetType;
    private final String uri;
    private final Optional<String> mainEntry;
    private final boolean isStrikingImage;
    private final ImmutableList<Representation> representations;

    private Asset(AssetType assetType, String uri, String mainEntry, boolean isStrikingImage, List<Representation> representations) {
      this.isStrikingImage = isStrikingImage;
      this.assetType = Preconditions.checkNotNull(assetType);
      this.uri = Preconditions.checkNotNull(uri);
      this.mainEntry = Optional.ofNullable(mainEntry);

      // Assert that all representations have unique names
      ImmutableMap<String, Representation> representationMap = Maps.uniqueIndex(representations, Representation::getName);
      this.representations = representationMap.values().asList();
    }

    public AssetType getAssetType() {
      return assetType;
    }

    public String getUri() {
      return uri;
    }

    public Optional<String> getMainEntry() {
      return mainEntry;
    }

    public boolean isStrikingImage() {
      return isStrikingImage;
    }

    public ImmutableList<Representation> getRepresentations() {
      return representations;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Asset asset = (Asset) o;
      return isStrikingImage == asset.isStrikingImage && assetType == asset.assetType && uri.equals(asset.uri)
          && mainEntry.equals(asset.mainEntry) && representations.equals(asset.representations);
    }

    @Override
    public int hashCode() {
      return 31 * (31 * (31 * (31 * assetType.hashCode() + uri.hashCode()) + mainEntry.hashCode())
          + (isStrikingImage ? 1 : 0)) + representations.hashCode();
    }
  }

  public static class Representation {
    private final String name;
    private final String entry;

    private Representation(String name, String entry) {
      this.name = Preconditions.checkNotNull(name);
      this.entry = Preconditions.checkNotNull(entry);
    }

    public String getName() {
      return name;
    }

    public String getEntry() {
      return entry;
    }

    // TODO: When the manifest supports explicitly declaring a CRepo key, return it.
    // If the manifest attribute is optional, the real implementation should remain Optional<String>.
    // This method should probably be inlined into getCrepoKey when this explanatory comment is no longer needed.
    private Optional<String> getDeclaredCrepoKey() {
      return Optional.empty(); // TODO
    }

    public String getCrepoKey() {
      return getDeclaredCrepoKey().orElse(entry);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Representation that = (Representation) o;
      return name.equals(that.name) && entry.equals(that.entry);
    }

    @Override
    public int hashCode() {
      return 31 * name.hashCode() + entry.hashCode();
    }
  }

}
