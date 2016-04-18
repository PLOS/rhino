package org.ambraproject.rhino.service.impl;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.AssetNodesByDoi;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.util.Archive;
import org.plos.crepo.model.RepoObject;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.ws.rs.core.MediaType;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ArticlePackageBuilder {

  private final Archive archive;
  private final ArticleXml article;
  private final ManifestXml manifest;
  private final String manifestEntry;
  private final String manuscriptEntry;
  private final String printableEntry;
  private final ArticleIdentity articleIdentity;

  ArticlePackageBuilder(Archive archive, ArticleXml article, ManifestXml manifest,
                        String manifestEntry, String manuscriptEntry, String printableEntry) {
    this.archive = Objects.requireNonNull(archive);
    this.article = Objects.requireNonNull(article);
    this.manifest = Objects.requireNonNull(manifest);
    this.manifestEntry = Objects.requireNonNull(manifestEntry);
    this.manuscriptEntry = Objects.requireNonNull(manuscriptEntry);
    this.printableEntry = Objects.requireNonNull(printableEntry);

    try {
      this.articleIdentity = article.readDoi();
    } catch (XmlContentException e) {
      throw new RuntimeException(e);
    }
  }

  public ArticlePackage build() {
    Map<String, RepoObject> articleObjects = buildArticleObjects();
    List<ScholarlyWork> assetWorks = buildAssetWorks(article.findAllAssetNodes());

    return new ArticlePackage(new ScholarlyWork(articleIdentity, articleObjects), assetWorks);
  }

  private Map<String, RepoObject> buildArticleObjects() {
    ImmutableMap.Builder<String, RepoObject> articleObjects = ImmutableMap.builder();
    articleObjects.put("manifest",
        new RepoObject.RepoObjectBuilder("manifest/" + articleIdentity.getIdentifier())
            .contentAccessor(archive.getContentAccessorFor(manifestEntry))
            .downloadName(manifestEntry)
            .contentType(MediaType.APPLICATION_XML)
            .build());
    articleObjects.put("manuscript",
        new RepoObject.RepoObjectBuilder("manuscript/" + articleIdentity.getIdentifier())
            .contentAccessor(archive.getContentAccessorFor(manuscriptEntry))
            .contentType(MediaType.APPLICATION_XML)
            .downloadName(articleIdentity.forXmlAsset().getFileName())
            .build());
    articleObjects.put("front",
        new RepoObject.RepoObjectBuilder("front/" + articleIdentity.getIdentifier())
            .byteContent(serializeXml(article.extractFrontMatter()))
            .contentType(MediaType.APPLICATION_XML)
            .build());
    articleObjects.put("frontAndBack",
        new RepoObject.RepoObjectBuilder("frontAndBack/" + articleIdentity.getIdentifier())
            .byteContent(serializeXml(article.extractFrontAndBackMatter()))
            .contentType(MediaType.APPLICATION_XML)
            .build());
    articleObjects.put("printable",
        new RepoObject.RepoObjectBuilder("printable/" + articleIdentity.getIdentifier())
            .contentAccessor(archive.getContentAccessorFor(printableEntry))
            .downloadName(printableEntry)
            .contentType("application/pdf")
            .build());
    return articleObjects.build();
  }

  private static byte[] serializeXml(Document document) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.transform(new DOMSource(document), new StreamResult(outputStream));
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return outputStream.toByteArray();
  }

  /**
   * Build an asset table from input being ingested.
   *
   * @param assetNodeMap encapsulated descriptions of references to asset DOIs in the manuscript
   * @return the built asset table
   */
  private List<ScholarlyWork> buildAssetWorks(AssetNodesByDoi assetNodeMap) {
    List<ScholarlyWork> works = new ArrayList<>();
    for (ManifestXml.Asset asset : manifest.parse()) {
      AssetType assetType = findAssetType(assetNodeMap, asset);
      if (assetType == AssetType.ARTICLE) continue;
      ImmutableMap.Builder<String, RepoObject> assetObjects = ImmutableMap.builder();
      AssetIdentity assetIdentity = AssetIdentity.create(asset.getUri());
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        String entryName = representation.getEntry();
        FileType fileType = assetType.getFileType(representation.getName());

        assetObjects.put(fileType.identifier,
            new RepoObject.RepoObjectBuilder(fileType.identifier + "/" + assetIdentity.getIdentifier())
                .contentAccessor(archive.getContentAccessorFor(entryName))
                .downloadName(entryName)
                .contentType(AssetFileIdentity.create(asset.getUri(), representation.getName()).inferContentType().toString())
                .build());
      }
      works.add(new ScholarlyWork(assetIdentity, assetObjects.build()));
    }
    return works;
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

  private static RuntimeException unmatchedReprException(String reprName) {
    String message = "Manifest contains unmatched \"repr\" value: " + reprName;
    return new RestClientException(message, HttpStatus.BAD_REQUEST);
  }

  /**
   * The type of an asset (with a unique asset DOI, represented by one or more files) as determined by where the asset
   * DOI is referenced in the manuscript of its parent article.
   */
  private static enum AssetType {
    // TODO: Remove?
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
        input -> input.identifier);
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
        input -> input.identifier);

    private static String unrecognizedFileType(String fileTypeIdentifier) {
      return String.format("File type not recognized: \"%s\". Must be one of: %s",
          fileTypeIdentifier, FileType.BY_IDENTIFIER.keySet());
    }
  }

}
