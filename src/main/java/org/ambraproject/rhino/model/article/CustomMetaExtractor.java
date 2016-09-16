package org.ambraproject.rhino.model.article;

import com.google.common.collect.ListMultimap;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public class CustomMetaExtractor {

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  public void apply(ArticleMetadata.Builder metadataBuilder, ListMultimap<String, String> customMeta) {
    RuntimeConfiguration.ManuscriptCustomMeta customMetaConfig = runtimeConfiguration.getManuscriptCustomMeta();

    getSingleValue(customMeta, customMetaConfig.getRevisionDateName()).ifPresent((String revisionDate) -> {
      LocalDate parsedDate;
      try {
        parsedDate = LocalDate.parse(revisionDate);
      } catch (DateTimeParseException e) {
        String message = String.format("'%s' custom-meta value must be an ISO-8601 date", customMetaConfig.getRevisionDateName());
        throw new XmlContentException(message, e);
      }
      metadataBuilder.setRevisionDate(parsedDate);
    });

    getSingleValue(customMeta, customMetaConfig.getPublicationStageName())
        .ifPresent(metadataBuilder::setPublicationStage);
  }

  private static Optional<String> getSingleValue(ListMultimap<String, String> customMeta, String key) {
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

}
