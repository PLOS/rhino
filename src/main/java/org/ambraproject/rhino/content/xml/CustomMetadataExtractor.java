package org.ambraproject.rhino.content.xml;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.model.article.ArticleCustomMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Node;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class CustomMetadataExtractor extends AbstractArticleXml<ArticleCustomMetadata> {
  private static final Logger log = LoggerFactory.getLogger(CustomMetadataExtractor.class);

  public static class Factory {
    @Autowired
    private RuntimeConfiguration runtimeConfiguration;

    public CustomMetadataExtractor parse(Node xml) {
      return new CustomMetadataExtractor(xml, runtimeConfiguration.getManuscriptCustomMeta());
    }
  }

  private final RuntimeConfiguration.ManuscriptCustomMeta customMetaNames;

  private CustomMetadataExtractor(Node xml, RuntimeConfiguration.ManuscriptCustomMeta customMetaNames) {
    super(xml);
    this.customMetaNames = Objects.requireNonNull(customMetaNames);
  }

  private static enum MetaTag {
    REVISION_DATE("manuscriptCustomMeta.revisionDate", RuntimeConfiguration.ManuscriptCustomMeta::getRevisionDateMetaTagName),
    PUBLICATION_STAGE("manuscriptCustomMeta.publicationStage", RuntimeConfiguration.ManuscriptCustomMeta::getPublicationStageMetaTagName);

    private final String description;
    private final Function<RuntimeConfiguration.ManuscriptCustomMeta, String> getter;

    private MetaTag(String description, Function<RuntimeConfiguration.ManuscriptCustomMeta, String> getter) {
      this.description = description;
      this.getter = getter;
    }
  }

  @Override
  public ArticleCustomMetadata build() throws XmlContentException {
    ListMultimap<String, String> customMeta = parseCustomMeta();
    ArticleCustomMetadata.Builder builder = ArticleCustomMetadata.builder();
    getSingleValue(customMeta, MetaTag.REVISION_DATE)
        .ifPresent(revisionDate -> builder.setRevisionDate(parseRevisionDate(revisionDate)));
    getSingleValue(customMeta, MetaTag.PUBLICATION_STAGE)
        .ifPresent(builder::setPublicationStage);
    return builder.build();
  }

  /**
   * Parse the {@code custom-meta} name-value pairs from the manuscript.
   * <p>
   * It is possible for the manuscript to contain multiple custom meta nodes with the same name, though this may be
   * invalid depending on the name.
   *
   * @return the multimap of {@code custom-meta} name-value pairs
   */
  private ListMultimap<String, String> parseCustomMeta() {
    List<Node> customMetaNodes = readNodeList("//custom-meta-group/custom-meta");
    ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();
    for (Node node : customMetaNodes) {
      String name = readString("child::meta-name", node);
      String value = sanitize(readString("child::meta-value", node));
      builder.put(name, value);
    }
    return builder.build();
  }

  private Optional<String> getSingleValue(ListMultimap<String, String> customMeta, MetaTag tag) {
    String key = tag.getter.apply(customMetaNames);
    if (key == null) {
      log.warn("No meta-name is configured for {}. Value cannot be parsed from manuscript or persisted on new ingestions.", tag.description);
      return Optional.empty();
    }

    List<String> values = customMeta.get(key);
    if (values.isEmpty()) {
      return Optional.empty();
    }
    if (values.size() > 1) {
      String message = String.format("Must not have more than one custom-meta node with <meta-name>%s</meta-name>. Values: %s",
          key, values);
      throw new XmlContentException(message);
    }
    return Optional.of(values.get(0));
  }

  private LocalDate parseRevisionDate(String revisionDate) {
    LocalDate parsedDate;
    try {
      parsedDate = LocalDate.parse(revisionDate);
    } catch (DateTimeParseException e) {
      String message = String.format("'%s' custom-meta value must be an ISO-8601 date", customMetaNames.getRevisionDateMetaTagName());
      throw new XmlContentException(message, e);
    }
    return parsedDate;
  }

}
