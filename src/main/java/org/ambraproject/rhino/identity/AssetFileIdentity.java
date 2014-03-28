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

package org.ambraproject.rhino.identity;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.ambraproject.filestore.FSIDMapper;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.util.ContentTypeInference;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.regex.Pattern;

/**
 * An identifier for one file that corresponds to an asset. It is uniquely identified by a DOI and file extension.
 */
public class AssetFileIdentity extends DoiBasedIdentity {

  private final String extension;

  private AssetFileIdentity(String identifier, String extension) {
    super(identifier);
    Preconditions.checkArgument(StringUtils.isNotBlank(extension));
    this.extension = extension;
  }

  /**
   * Parse the identifier from a path. The input is from URL that a REST client would use to identify an entity.
   *
   * @param path the full path variable from the URL that identifies the entity
   * @return an identifier object for the entity
   * @see org.ambraproject.rhino.rest.controller.abstr.RestController#getFullPathVariable
   */
  public static AssetFileIdentity parse(String path) {
    int dotIndex = path.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex + 1 >= path.length()) {
      throw new IllegalArgumentException("Request URI does not have file extension");
    }
    String identifier = path.substring(0, dotIndex);
    String extension = path.substring(dotIndex + 1);

    return new AssetFileIdentity(identifier, extension);
  }

  public static AssetFileIdentity create(String identifier, String extension) {
    return new AssetFileIdentity(asIdentifier(identifier), extension);
  }

  /**
   * Check whether an asset has been initialized with a file representation.
   *
   * @param asset the asset
   * @return {@code true} if the asset has an associated file
   */
  public static boolean hasFile(ArticleAsset asset) {
    return !Strings.isNullOrEmpty(asset.getExtension());
  }

  /**
   * Create an identity representing an asset object with a file representation.
   *
   * @param asset the asset
   * @return the identity
   * @throws IllegalArgumentException if the asset does not have a file (i.e., if {@code !hasFile(asset)})
   */
  public static AssetFileIdentity from(ArticleAsset asset) {
    Preconditions.checkArgument(hasFile(asset), "Asset is not associated with a file");
    return AssetFileIdentity.create(asset.getDoi(), asset.getExtension());
  }

  /**
   * Get the file extension for the file associated with the identified asset in the file store. File extensions are
   * treated as case-insensitive, so any letters in the returned value are lowercase.
   *
   * @return the file extension
   */
  public String getFileExtension() {
    return extension;
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
    return getIdentifier() + '.' + getFileExtension();
  }

  /**
   * Return the portion of the full file path that, if it were a path on an actual file system, would be the name of the
   * file. In other words, everything after the final {@code '/'} character, or the full string if there is none.
   * <p/>
   * Note that DOIs are not file paths and that slashes within them do not necessarily denote a directory hierarchy.
   *
   * @return the file name
   */
  public String getFileName() {
    String identifier = getIdentifier();
    int lastSlashIndex = identifier.lastIndexOf('/');
    String fileName = (lastSlashIndex < 0) ? identifier : identifier.substring(lastSlashIndex + 1);
    return fileName + '.' + getFileExtension();
  }

  /**
   * PLOS idiosyncrasy: this file extension is used for PNG images.
   */
  private static final Pattern PNG_THUMBNAIL = Pattern.compile("PNG_\\w", Pattern.CASE_INSENSITIVE);

  /**
   * Get the content type for the data associated with the identified entity in the file store. The returned value is a
   * Spring object that maps onto a standard MIME type.
   *
   * @return the content type
   */
  public MediaType getContentType() {
    if (PNG_THUMBNAIL.matcher(getFileExtension()).matches()) {
      return MediaType.IMAGE_PNG;
    }
    String mimeType = ContentTypeInference.inferContentType(getFilePath());
    return MediaType.parseMediaType(mimeType);
  }

  /**
   * Get file store identifier for the data associated with the article or asset that this object identifies.
   *
   * @return the FSID (file store identifier)
   * @throws org.ambraproject.rhino.rest.RestClientException if the DOI can't be parsed and converted into an FSID
   */
  public String getFsid() {
    String fsid = FSIDMapper.doiTofsid(getKey(), getFileExtension());
    if (fsid.isEmpty()) {
      throw new RestClientException("DOI does not match expected format", HttpStatus.BAD_REQUEST);
    }
    return fsid;
  }

  /**
   * Get the identity of the asset to which this file belongs.
   *
   * @return the asset's identity
   */
  public AssetIdentity forAsset() {
    return AssetIdentity.create(getIdentifier());
  }

  /**
   * If this asset is an XML file, return the identity of the article to which it would belong <em>if</em> it is an
   * article's NLM DTD file.
   *
   * @return
   */
  public Optional<ArticleIdentity> forArticle() {
    if (XML_EXTENSION.equalsIgnoreCase(extension)) {
      return Optional.of(ArticleIdentity.create(getIdentifier()));
    }
    return Optional.absent();
  }

  @Override
  public String toString() {
    return getFilePath();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    AssetFileIdentity that = (AssetFileIdentity) o;

    if (extension != null ? !extension.equals(that.extension) : that.extension != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (extension != null ? extension.hashCode() : 0);
    return result;
  }

}
