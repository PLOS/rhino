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

package org.ambraproject.admin.identity;

import org.ambraproject.admin.RestClientException;
import org.ambraproject.admin.util.ImmutableMimetypesFileTypeMap;
import org.ambraproject.filestore.FSIDMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class AssetIdentity extends DoiBasedIdentity {

  private static final ImmutableMimetypesFileTypeMap MIMETYPES = new ImmutableMimetypesFileTypeMap();

  private final String extension; // non-empty and contains no uppercase letters

  AssetIdentity(String identifier, String extension) {
    super(identifier);
    this.extension = extension.toLowerCase();
  }

  /**
   * Parse the identifier from a path. The input is from URL that a REST client would use to identify an entity.
   *
   * @param path the full path variable from the URL that identifies the entity
   * @return an identifier object for the entity
   * @see org.ambraproject.admin.controller.RestController#getFullPathVariable
   */
  public static AssetIdentity parse(String path) {
    int dotIndex = path.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex + 1 >= path.length()) {
      throw new IllegalArgumentException("Request URI does not have file extension");
    }
    String identifier = path.substring(0, dotIndex);
    String extension = path.substring(dotIndex + 1);

    return new AssetIdentity(identifier, extension);
  }

  @Override
  public String getName() {
    return getFilePath();
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
   * Get the content type for the data associated with the identified entity in the file store. The returned value is a
   * Spring object that maps onto a standard MIME type.
   *
   * @return the content type
   */
  public MediaType getContentType() {
    if (getFileExtension().equalsIgnoreCase(XML_EXTENSION)) {
      return MediaType.TEXT_XML;
    }
    String mimeType = MIMETYPES.getContentType(getFilePath());
    return MediaType.parseMediaType(mimeType);
  }

  /**
   * Get file store identifier for the data associated with the article or asset that this object identifies.
   *
   * @return the FSID (file store identifier)
   * @throws org.ambraproject.admin.RestClientException
   *          if the DOI can't be parsed and converted into an FSID
   */
  public String getFsid() {
    String fsid = FSIDMapper.doiTofsid(getKey(), getFileExtension());
    if (fsid.isEmpty()) {
      throw new RestClientException("DOI does not match expected format", HttpStatus.BAD_REQUEST);
    }
    return fsid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    AssetIdentity that = (AssetIdentity) o;

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
