/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.model.ingest;

import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.AssetNodesByDoi;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.util.ContentTypeInference;
import org.plos.crepo.model.input.RepoObjectInput;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ArticlePackageBuilder {

  private final String destinationBucketName;
  private final Archive archive;
  private final ArticleXml article;
  private final ManifestXml manifest;
  private final ManifestXml.Asset manuscriptAsset;
  private final ManifestXml.Representation manuscriptRepr;
  private final Optional<ManifestXml.Representation> printableRepr;
  private final Doi articleIdentity;

  public ArticlePackageBuilder(String destinationBucketName, Archive archive,
                               ArticleXml article, ManifestXml manifest) {
    this.destinationBucketName = Objects.requireNonNull(destinationBucketName);
    this.archive = Objects.requireNonNull(archive);
    this.article = Objects.requireNonNull(article);
    this.manifest = Objects.requireNonNull(manifest);

    this.manuscriptAsset = Objects.requireNonNull(manifest.getArticleAsset());
    this.manuscriptRepr = Objects.requireNonNull(manuscriptAsset.getRepresentation("manuscript").get());
    this.printableRepr = Objects.requireNonNull(manuscriptAsset.getRepresentation("printable"));

    try {
      this.articleIdentity = article.readDoi();
    } catch (XmlContentException e) {
      throw new RuntimeException(e);
    }
  }

  public ArticlePackage build() {
    Map<String, ArticleFileInput> articleObjects = buildArticleObjects();
    List<ArticleItemInput> assetItems = buildAssetItems(article.findAllAssetNodes());
    List<ArticleFileInput> ancillaryFiles = manifest.getAncillaryFiles().stream()
        .map(this::buildObjectForAncillary).collect(Collectors.toList());

    return new ArticlePackage(new ArticleItemInput(articleIdentity, articleObjects,
        AssetType.ARTICLE.getIdentifier()), assetItems, ancillaryFiles, manifest,
        destinationBucketName);
  }

  private ArticleFileInput buildObjectForAsset(ManifestXml.Asset asset, ManifestXml.Representation representation) {
    ManifestXml.ManifestFile manifestFile = representation.getFile();
    String mimetype = manifestFile.getMimetype();
    String downloadName = generateDownloadName(asset.getUri(), representation.getFile().getEntry());
    return buildObject(manifestFile, downloadName, mimetype);
  }

  private ArticleFileInput buildObjectForAncillary(ManifestXml.ManifestFile manifestFile) {
    String mimetype = manifestFile.getMimetype();
    if (mimetype == null) {
      mimetype = ContentTypeInference.inferContentType(manifestFile.getEntry());
    }
    return buildObject(manifestFile, manifestFile.getEntry(), mimetype);
  }

  private ArticleFileInput buildObject(ManifestXml.ManifestFile manifestFile, String downloadName, String contentType) {
    String filename = manifestFile.getEntry();
    RepoObjectInput repoObjectInput = RepoObjectInput.builder(destinationBucketName, manifestFile.getCrepoKey())
        .setContentAccessor(archive.getContentAccessorFor(filename))
        .setContentType(contentType)
        .setDownloadName(downloadName)
        .build();
    return ArticleFileInput.create(filename, repoObjectInput);
  }

  private static String generateDownloadName(String doi, String filename) {
    String name = getLastToken(doi, '/');
    String extension = getLastToken(filename, '.');
    return name + "." + sanitizePngExtension(extension);
  }

  private static String getLastToken(String s, char delimiter) {
    return s.substring(s.lastIndexOf(delimiter) + 1);
  }

  // TODO: Deduplicate with Wombat
  private static final Pattern PNG_THUMBNAIL_PATTERN = Pattern.compile("(PNG)_\\w+", Pattern.CASE_INSENSITIVE);

  private static String sanitizePngExtension(String extension) {
    Matcher matcher = PNG_THUMBNAIL_PATTERN.matcher(extension);
    return matcher.matches() ? matcher.group(1) : extension;
  }

  private Map<String, ArticleFileInput> buildArticleObjects() {
    ImmutableMap.Builder<String, ArticleFileInput> articleObjects = ImmutableMap.builder();
    articleObjects.put("manuscript", buildObjectForAsset(manuscriptAsset, manuscriptRepr));
    if (printableRepr.isPresent()) {
      articleObjects.put("printable", buildObjectForAsset(manuscriptAsset, printableRepr.get()));
    }
    return articleObjects.build();
  }

  /**
   * Build an asset table from input being ingested.
   *
   * @param assetNodeMap encapsulated descriptions of references to asset DOIs in the manuscript
   * @return the built asset table
   */
  private List<ArticleItemInput> buildAssetItems(AssetNodesByDoi assetNodeMap) {
    List<ArticleItemInput> items = new ArrayList<>();
    for (ManifestXml.Asset asset : manifest.getAssets()) {
      AssetType assetType = findAssetType(assetNodeMap, asset);
      if (assetType == AssetType.ARTICLE) continue;
      ImmutableMap.Builder<String, ArticleFileInput> assetObjects = ImmutableMap.builder();
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        FileType fileType = assetType.getFileType(representation.getType());
        ArticleFileInput fileInput = buildObjectForAsset(asset, representation);
        assetObjects.put(fileType.getIdentifier(), fileInput);
      }
      items.add(new ArticleItemInput(Doi.create(asset.getUri()), assetObjects.build(), assetType.getIdentifier()));
    }
    return items;
  }

  private static AssetType findAssetType(AssetNodesByDoi assetNodeMap, ManifestXml.Asset asset) {
    if (asset.getAssetTagName().equals(ManifestXml.AssetTagName.ARTICLE)) {
      return AssetType.ARTICLE;
    }
    Doi assetIdentity = Doi.create(asset.getUri());
    if (!assetNodeMap.getDois().contains(assetIdentity)) {
      if (asset.isStrikingImage()) {
        return AssetType.STANDALONE_STRIKING_IMAGE;
      } else {
        throw new RestClientException("Asset not mentioned in manuscript: " + asset.getUri(), HttpStatus.BAD_REQUEST);
      }
    }

    List<Node> nodes = assetNodeMap.getNodes(assetIdentity);
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

}
