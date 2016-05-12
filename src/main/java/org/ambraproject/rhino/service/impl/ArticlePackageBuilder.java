package org.ambraproject.rhino.service.impl;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.util.Archive;
import org.plos.crepo.model.RepoObject;
import org.w3c.dom.Document;

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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

class ArticlePackageBuilder {

  private final Archive archive;
  private final ArticleXml article;
  private final ManifestXml manifest;
  private final String manifestEntry;
  private final ManifestXml.Representation manuscriptRepr;
  private final ManifestXml.Representation printableRepr;
  private final ArticleIdentity articleIdentity;

  ArticlePackageBuilder(Archive archive, ArticleXml article, ManifestXml manifest, String manifestEntry,
                        ManifestXml.Representation manuscriptRepr, ManifestXml.Representation printableRepr) {
    this.archive = Objects.requireNonNull(archive);
    this.article = Objects.requireNonNull(article);
    this.manifest = Objects.requireNonNull(manifest);
    this.manifestEntry = Objects.requireNonNull(manifestEntry);
    this.manuscriptRepr = Objects.requireNonNull(manuscriptRepr);
    this.printableRepr = Objects.requireNonNull(printableRepr);

    try {
      this.articleIdentity = article.readDoi();
    } catch (XmlContentException e) {
      throw new RuntimeException(e);
    }
  }

  public ArticlePackage build() {
    Map<String, RepoObject> articleObjects = buildArticleObjects();
    List<ScholarlyWorkInput> assetWorks = buildAssetWorks();

    return new ArticlePackage(new ScholarlyWorkInput(articleIdentity, articleObjects, AssetType.ARTICLE.identifier), assetWorks);
  }

  private RepoObject buildObjectFor(ManifestXml.ManifestFile manifestFile) {
    return new RepoObject.RepoObjectBuilder(manifestFile.getCrepoKey())
        .contentAccessor(archive.getContentAccessorFor(manifestFile.getEntry()))
        .contentType(manifestFile.getMimetype())
        .downloadName(manifestFile.getEntry())
        .build();
  }

  private Map<String, RepoObject> buildArticleObjects() {
    ImmutableMap.Builder<String, RepoObject> articleObjects = ImmutableMap.builder();
    articleObjects.put("manifest",
        new RepoObject.RepoObjectBuilder("manifest/" + articleIdentity.getIdentifier())
            .contentAccessor(archive.getContentAccessorFor(manifestEntry))
            .downloadName(manifestEntry)
            .contentType(MediaType.APPLICATION_XML)
            .build());
    articleObjects.put("manuscript", buildObjectFor(manuscriptRepr.getFile()));
    articleObjects.put("printable", buildObjectFor(printableRepr.getFile()));

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
   * @return the built asset table
   */
  private List<ScholarlyWorkInput> buildAssetWorks() {
    List<ScholarlyWorkInput> works = new ArrayList<>();
    for (ManifestXml.Asset asset : manifest.getAssets()) {
      AssetType assetType = ASSET_TYPE_DICTIONARY.get(asset.getType());
      if (assetType == AssetType.ARTICLE) continue;
      AssetIdentity assetIdentity = AssetIdentity.create(asset.getUri());

      Set<FileType> fileTypes = new HashSet<>();
      Map<String, RepoObject> assetObjects = new LinkedHashMap<>();
      for (ManifestXml.Representation representation : asset.getRepresentations()) {
        FileType fileType = FILE_TYPE_DICTIONARY.get(representation.getType());
        fileTypes.add(fileType);
        RepoObject previous = assetObjects.put(getKey(fileType), buildObjectFor(representation.getFile()));
        if (previous != null) {
          throw new ArticlePackageException(String.format("Multiple files of type %s on asset %s", fileType, assetType));
        }
      }
      validate(assetType, fileTypes);
      works.add(new ScholarlyWorkInput(assetIdentity, assetObjects, assetType.identifier));
    }
    return works;
  }

  private static void validate(AssetType assetType, Set<FileType> fileTypes) {
    if (!fileTypes.containsAll(assetType.getRequiredFileTypes())) {
      throw new ArticlePackageException(
          String.format("Not all required files for %s are present. Missing: %s",
              assetType, Sets.difference(assetType.getRequiredFileTypes(), fileTypes)));
    }
    if (!assetType.getSupportedFileTypes().containsAll(fileTypes)) {
      throw new ArticlePackageException(
          String.format("Unsupported file types for %s: %s",
              assetType, Sets.difference(fileTypes, assetType.getSupportedFileTypes())));
    }
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
    },

    FIGURE {
      @Override
      protected ImmutableSet<FileType> getSupportedFileTypes() {
        return STANDARD_THUMBNAIL_FILE_TYPES;
      }
    },

    TABLE {
      @Override
      protected ImmutableSet<FileType> getSupportedFileTypes() {
        return STANDARD_THUMBNAIL_FILE_TYPES;
      }
    },

    GRAPHIC {
      private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.ORIGINAL, FileType.THUMBNAIL);

      @Override
      protected ImmutableSet<FileType> getSupportedFileTypes() {
        return TYPES;
      }
    },

    SUPPLEMENTARY_MATERIAL {
      private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.SUPPLEMENTARY);

      @Override
      protected ImmutableSet<FileType> getSupportedFileTypes() {
        return TYPES;
      }
    },

    STANDALONE_STRIKING_IMAGE {
      private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.STRIKING_IMAGE);

      @Override
      protected ImmutableSet<FileType> getSupportedFileTypes() {
        return TYPES;
      }
    };

    // Shared by FIGURE and TABLE
    private static final ImmutableSet<FileType> STANDARD_THUMBNAIL_FILE_TYPES = Sets.immutableEnumSet(
        FileType.ORIGINAL, FileType.SMALL, FileType.INLINE, FileType.MEDIUM, FileType.LARGE);

    private final String identifier = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());

    /**
     * Return the set of file types that an ingestible may supply for each asset of this type.
     */
    protected abstract ImmutableSet<FileType> getSupportedFileTypes();

    /**
     * Return the set of file types that an ingestible must supply for each asset of this type. Must be a subset of what
     * is returned from {@link #getSupportedFileTypes}.
     */
    protected ImmutableSet<FileType> getRequiredFileTypes() {
      return getSupportedFileTypes();
    }
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
  }


  public static class ArticlePackageException extends RuntimeException {
    private ArticlePackageException(String message) {
      super(message);
    }

    private ArticlePackageException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  private static String getKey(Enum<?> value) {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, value.name());
  }

  private static class EnumNameDictionary<E extends Enum<E>> {
    private final ImmutableSortedMap<String, E> map;

    private EnumNameDictionary(Class<E> enumType) {
      this.map = ImmutableSortedMap.copyOf(
          EnumSet.allOf(enumType).stream()
              .collect(Collectors.toMap(ArticlePackageBuilder::getKey, Function.identity())),
          String.CASE_INSENSITIVE_ORDER);
    }

    public E get(String name) {
      E value = map.get(name);
      if (value == null) {
        String message = String.format("Type not recognized: \"%s\". Must be one of: %s",
            name, map.keySet());
        throw new ArticlePackageException(message);
      }
      return value;
    }
  }

  private static final EnumNameDictionary<AssetType> ASSET_TYPE_DICTIONARY = new EnumNameDictionary<>(AssetType.class);
  private static final EnumNameDictionary<FileType> FILE_TYPE_DICTIONARY = new EnumNameDictionary<>(FileType.class);

}
