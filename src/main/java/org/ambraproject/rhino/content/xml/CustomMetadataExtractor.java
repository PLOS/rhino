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

package org.ambraproject.rhino.content.xml;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import org.ambraproject.rhino.model.article.ArticleCustomMetadata;
import org.w3c.dom.Node;

/**
 * Parses {@code &lt;custom-meta&gt;} elements from a manuscript, looking for {@code &lt;meta-name&gt;} values that are
 * configured per server.
 */
public class CustomMetadataExtractor extends AbstractArticleXml<ArticleCustomMetadata> {
  public static class Factory {
    public CustomMetadataExtractor parse(Node xml) {
      return new CustomMetadataExtractor(xml);
    }
  }

  private CustomMetadataExtractor(Node xml) {
    super(xml);
  }

  @Override
  public ArticleCustomMetadata build() throws XmlContentException {
    ListMultimap<String, String> customMeta = parseCustomMeta();
    ArticleCustomMetadata.Builder builder = ArticleCustomMetadata.builder();
    getSingleValue(customMeta, "Publication Update")
      .map(this::parseRevisionDate)
      .ifPresent(builder::setRevisionDate);
    getSingleValue(customMeta, "PLOS Publication Stage").ifPresent(builder::setPublicationStage);
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
      String value = Strings.nullToEmpty(sanitize(readString("child::meta-value", node)));
      builder.put(name, value);
    }
    return builder.build();
  }

  /**
   * Read a custom meta value, expecting at most one.
   * <p>
   * The return value will be empty if the manuscript does not have a value of the given type <em>or</em> if no
   * meta-name is configured for that type.
   *
   * @param customMeta the table of all of a manuscript's custom meta values
   * @param attribute  the type of value to retrieve, if a meta-name is configured for it
   * @return the value if the type is configured and it is present
   */
  private Optional<String> getSingleValue(ListMultimap<String, String> customMeta, String metaName) {
    Preconditions.checkNotNull(metaName);

    List<String> values = customMeta.get(metaName);
    if (values.isEmpty()) {
      return Optional.empty();
    }
    if (values.size() > 1) {
      String message = String.format("Must not have more than one custom-meta node with <meta-name>%s</meta-name>. Values: %s",
          metaName, values);
      throw new XmlContentException(message);
    }
    return Optional.of(values.get(0));
  }

  private LocalDate parseRevisionDate(String revisionDate) {
    LocalDate parsedDate;
    try {
      parsedDate = LocalDate.parse(revisionDate);
    } catch (DateTimeParseException e) {
      String message = String.format("'%s' custom-meta value must be an ISO-8601 date");
      throw new XmlContentException(message, e);
    }
    return parsedDate;
  }

}
