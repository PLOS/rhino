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
                               ArticleXml article, ManifestXml manifest, ManifestXml.Asset manuscriptAsset,
                               ManifestXml.Representation manuscriptRepr, Optional<ManifestXml.Representation> printableRepr) {
    this.destinationBucketName = Objects.requireNonNull(destinationBucketName);
    this.archive = Objects.requireNonNull(archive);
    this.article = Objects.requireNonNull(article);
    this.manifest = Objects.requireNonNull(manifest);

    this.manuscriptAsset = Objects.requireNonNull(manuscriptAsset);
    this.manuscriptRepr = Objects.requireNonNull(manuscriptRepr);
    this.printableRepr = Objects.requireNonNull(printableRepr);

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
        .map(this::buildObjectFor).collect(Collectors.toList());

    return new ArticlePackage(new ArticleItemInput(articleIdentity, articleObjects, AssetType.ARTICLE.getIdentifier()),
        assetItems, ancillaryFiles);
  }

  private ArticleFileInput buildObjectFor(ManifestXml.Asset asset, ManifestXml.Representation representation) {
    ManifestXml.ManifestFile manifestFile = representation.getFile();
    String mimetype = manifestFile.getMimetype();
    return buildObject(manifestFile, mimetype);
  }

  private ArticleFileInput buildObjectFor(ManifestXml.ManifestFile manifestFile) {
    String mimetype = manifestFile.getMimetype();
    if (mimetype == null) {
      mimetype = ContentTypeInference.inferContentType(manifestFile.getEntry());
    }
    return buildObject(manifestFile, mimetype);
  }

  private ArticleFileInput buildObject(ManifestXml.ManifestFile manifestFile, String contentType) {
    String filename = manifestFile.getEntry();
    RepoObjectInput repoObjectInput = RepoObjectInput.builder(destinationBucketName, manifestFile.getCrepoKey())
        .setContentAccessor(archive.getContentAccessorFor(filename))
        .setContentType(contentType)
        .setDownloadName(filename)
        .build();
    return new ArticleFileInput(filename, repoObjectInput);
  }

  private Map<String, ArticleFileInput> buildArticleObjects() {
    ImmutableMap.Builder<String, ArticleFileInput> articleObjects = ImmutableMap.builder();
    articleObjects.put("manuscript", buildObjectFor(manuscriptAsset, manuscriptRepr));
    if (printableRepr.isPresent()) {
      articleObjects.put("printable", buildObjectFor(manuscriptAsset, printableRepr.get()));
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
      Doi assetIdentity = Doi.create(asset.getUri());
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        FileType fileType = assetType.getFileType(representation.getType());
        assetObjects.put(fileType.getIdentifier(), buildObjectFor(representation.getFile()));
      }
      items.add(new ArticleItemInput(assetIdentity, assetObjects.build(), assetType.getIdentifier()));
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
