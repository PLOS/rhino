/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.admin.controller;

import com.google.common.base.Preconditions;
import org.ambraproject.admin.RestClientException;
import org.ambraproject.filestore.FSIDMapper;
import org.ambraproject.models.Article;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.activation.MimetypesFileTypeMap;

/**
 * An entity identifier based on a Digital Object Identifier (DOI). Instances of this class cover two cases: <ol>
 * <li>The entity is an article, in which case the {@link #getIdentifier() identifier} is the article's actual DOI that
 * would be known to public DOI resolvers.</li> <li>The entity is an asset of an article. In this case, the {@link
 * #getIdentifier() identifier} is not a true DOI, but uses the parent article's DOI as a prefix. In this sense it is
 * "DOI-based".</li> </ol>
 */
public class DoiBasedIdentity {

  private static final String DOI_SCHEME_VALUE = "info:doi/";
  private static final String XML_EXTENSION = "xml";
  private static final MimetypesFileTypeMap MIMETYPES = new MimetypesFileTypeMap();


  /*
   * Internal invariants:
   *   - Instances of this class are immutable.
   *   - All fields are non-null (but the Optional object may have an "absent" state, of course).
   *   - The string fields are non-empty (they have length > 0).
   *   - The identifier field does not contain ':'.
   *   - The extension field contains no uppercase letters.
   *   - The static MIMETYPES object's state is never mutated.
   */

  private final String identifier;
  private final String extension;

  private DoiBasedIdentity(String identifier, String extension) {
    super();
    this.identifier = Preconditions.checkNotNull(identifier);
    this.extension = Preconditions.checkNotNull(extension).toLowerCase();

    validate();
  }

  private void validate() {
    Preconditions.checkArgument(identifier.indexOf(':') < 0, "DOI must not have scheme prefix (\"info:doi/\")");
    Preconditions.checkArgument(!identifier.isEmpty(), "DOI is an empty string");
    Preconditions.checkArgument(!extension.isEmpty(), "Extension is an empty string");
  }

  /**
   * Create an identifier for an article. The data described by the identifier is the article's XML file.
   * <p/>
   * The DOI provided must <em>not</em> be prefixed with {@code "info:doi/"} or any other URI scheme value.
   *
   * @param identifier the article's DOI
   * @return an identifier for the article
   * @throws IllegalArgumentException if the DOI is prefixed with a URI scheme value or is null or empty
   */
  public static DoiBasedIdentity forArticle(String identifier) {
    return new DoiBasedIdentity(identifier, XML_EXTENSION);
  }

  /**
   * Create an identifier for an article entity. The data described by the identifier is the article's XML file.
   *
   * @param article an article
   * @return an identifier for the article
   * @throws IllegalArgumentException if the article's DOI is uninitialized or does not start with the expected scheme
   *                                  value
   */
  public static DoiBasedIdentity forArticle(Article article) {
    String doi = article.getDoi();
    if (doi == null || !doi.startsWith(DOI_SCHEME_VALUE)) {
      throw new IllegalArgumentException("Article does not have expected DOI: " + doi);
    }
    String identifier = doi.substring(DOI_SCHEME_VALUE.length());
    return forArticle(identifier);
  }

  /**
   * Parse the identifier from a RESTful request directed at an object in the article namespace.
   *
   * @param requestUri the location at which the RESTful request to parse was received
   * @return an for the article or asset to which the REST action was directed
   * @see ArticleCrudController
   */
  public static DoiBasedIdentity parse(String requestUri, String namespace) {
    if (!requestUri.startsWith(namespace)) {
      // Valid controller mappings should prevent this
      throw new IllegalArgumentException("Request URI prefixed with wrong namespace");
    }
    int dotIndex = requestUri.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex + 1 >= requestUri.length()) {
      throw new IllegalArgumentException("Request URI does not have file extension");
    }
    String identifier = requestUri.substring(namespace.length(), dotIndex);
    String extension = requestUri.substring(dotIndex + 1);

    return new DoiBasedIdentity(identifier, extension);
  }

  /**
   * Return the DOI or DOI-like identifier for the article or asset that this object identifies. The return value will
   * not be prefixed with {@code "info:doi/"} and, under Ambra's current data model, must not be stored in a DOI column
   * of any database table.
   * <p/>
   * The value {@code this.getIdentifier()} represents an actual DOI (that a public DOI resolver would know about) if
   * and only if {@code this.isAsset()} returns {@code false}.
   *
   * @return the DOI or DOI-like identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Return a database key value to represent the article or asset that this object identifies. Under Ambra's current
   * data model, this is the string returned by {@link #getIdentifier} prefixed with {@code "info:doi/"}.
   *
   * @return the key value
   */
  public String getKey() {
    return DOI_SCHEME_VALUE + identifier;
  }

  /**
   * Return the virtual file path to the article or asset that this object identifies. The returned string would appear
   * at the end of a RESTful URL that the client uses to refer to the article or asset. (That is, the identified object
   * would have
   * <pre>"http://" + theHostname + "/article/" + this.getFilePath()</pre>
   * for a URL.)
   *
   * @return the file path
   */
  public String getFilePath() {
    return identifier + '.' + extension;
  }

  /**
   * Get the file extension for the data associated with the identified entity in the file store. File extensions are
   * treated as case-insensitive, so any letters in the returned value are lowercase.
   * <p/>
   * If this object identifies an article, then the associated data is the NLM-format article XML and this method
   * returns {@code "xml"}. If this object identifies an asset, then the file extension represents the data type of that
   * asset.
   *
   * @return the file extension
   */
  public String getExtension() {
    return extension;
  }

  /**
   * Get the content type for the data associated with the identified entity in the file store. The returned value is a
   * Spring object that maps onto a standard MIME type.
   *
   * @return the content type
   */
  public MediaType getContentType() {
    if (extension.equalsIgnoreCase(XML_EXTENSION)) {
      return MediaType.TEXT_XML;
    }
    String mimeType = MIMETYPES.getContentType(getFilePath());
    return MediaType.parseMediaType(mimeType);
  }

  /**
   * Get file store identifier for the data associated with the article or asset that this object identifies.
   *
   * @return the FSID (file store identifier)
   * @throws RestClientException if the DOI can't be parsed and converted into an FSID
   */
  public String getFsid() {
    String fsid = FSIDMapper.doiTofsid(getKey(), extension);
    if (fsid.isEmpty()) {
      throw new RestClientException("DOI does not match expected format", HttpStatus.BAD_REQUEST);
    }
    return fsid;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DoiBasedIdentity that = (DoiBasedIdentity) o;

    if (!identifier.equals(that.identifier)) return false;
    if (!extension.equals(that.extension)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + identifier.hashCode();
    result = prime * result + extension.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('(');
    sb.append("identifier=\"").append(identifier).append('\"');
    sb.append(", extension=\"").append(extension).append('\"');
    sb.append(')');
    return sb.toString();
  }

}
