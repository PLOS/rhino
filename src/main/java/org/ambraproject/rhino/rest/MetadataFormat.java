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

package org.ambraproject.rhino.rest;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.MediaType;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * A format for entity metadata, that can be sent to the client in response to a GET request.
 */
public enum MetadataFormat {
  /*
   * For now, this is a singleton enum, as the application only supports output in JSON.
   * But, more values may be added here in case more formats are supported in the future.
   *
   * Note that the static "get" methods assume that the same set of formats (i.e., everything in this enum)
   * is supported for all operations. It may be necessary to change those static methods if a format is added,
   * but supported only for some operations and not others.
   */

  JSON(
      MediaType.JSON_UTF_8, // for regular JSON
      MediaType.JAVASCRIPT_UTF_8 // for JSONP
  );

  private final ImmutableSet<MediaType> mediaTypes;

  private MetadataFormat(MediaType... mediaTypes) {
    this.mediaTypes = ImmutableSet.copyOf(mediaTypes);
  }

  private static final ImmutableMap<String, MetadataFormat> BY_CONTENT_TYPE = buildContentTypeMap();

  private static ImmutableMap<String, MetadataFormat> buildContentTypeMap() {
    ImmutableMap.Builder<String, MetadataFormat> contentTypeMap = ImmutableMap.builder();
    for (MetadataFormat metadataFormat : values()) {
      for (MediaType mediaType : metadataFormat.mediaTypes) {
        String contentTypeString = upToFirstSemicolon(mediaType.toString());
        contentTypeMap.put(contentTypeString, metadataFormat);
      }
    }
    return contentTypeMap.build();
  }


  /**
   * The name of the HTTP header by which the client selects a metadata format.
   */
  private static final String ACCEPT_HEADER_NAME = "Accept";

  /**
   * The default format to use, if an "Accept" header is missing or names only unsupported formats.
   */
  private static final MetadataFormat DEFAULT = JSON;

  /**
   * Check if a content type value from an "Accept" header is a wildcard that matches {@link #DEFAULT}. (Correctness of
   * behavior depends on the value of the {@code DEFAULT} constant, and must be updated if the constant is changed.)
   *
   * @param contentType a content type value from an "Accept" header
   * @return {@code true} if the content type is a wildcard and it matches {@link #DEFAULT}
   */
  private static boolean isWildcardMatchingDefault(String contentType) {
    return "*/*".equals(contentType) || "application/*".equals(contentType);
  }

  // Utility for parsing header values
  private static String upToFirstSemicolon(String s) {
    int index = s.indexOf(';');
    return (index < 0) ? "" : s.substring(0, index);
  }

  private static final Splitter CONTENT_TYPE_SPLITTER = Splitter.on(',');


  /**
   * Provide a metadata format for a {@code Accept} header value. Match the content types listed in the header to the
   * first available supported format, or return the default format if none of the formats listed is supported. If there
   * is no {@code Accept} header, return the default format.
   * <p/>
   * This is the preferred way to get the metadata format in a controller method that doesn't otherwise need to receive
   * an {@code HttpServletRequest} or {@code HttpHeaders} object from Spring.
   * <p/>
   * Example usage in controller:
   * <pre>   @RequestMapping
   *   public void serve(@RequestHeader(value = "Accept", required = false) String accept) {
   *     MetadataFormat mf = getFromAcceptHeader(accept);
   *   }
   * </pre>
   *
   * @param acceptHeaderValue the {@code Accept} header value from a request
   * @return the metadata format to use
   */
  public static MetadataFormat getFromAcceptHeader(String acceptHeaderValue) {
    if (Strings.isNullOrEmpty(acceptHeaderValue)) return DEFAULT;
    String contentTypeList = upToFirstSemicolon(acceptHeaderValue);
    for (String contentType : CONTENT_TYPE_SPLITTER.split(contentTypeList)) {
      contentType = contentType.trim();
      if (isWildcardMatchingDefault(contentType)) return DEFAULT;
      MetadataFormat metadataFormat = BY_CONTENT_TYPE.get(contentType);
      if (metadataFormat != null) {
        return metadataFormat;
      }
    }
    return DEFAULT;
  }

  /**
   * Provide a metadata format for a request. Match the content types listed in the {@code Accept} header to the first
   * available supported format, or return the default format if none of the formats listed is supported. If there is no
   * {@code Accept} header, return the default format.
   * <p/>
   * In general, controllers should call this method only if they already receive an {@code HttpServletRequest} from
   * Spring for some other reason. If you access the headers only to get the metadata format, prefer {@link
   * #getFromAcceptHeader(String)}.
   * <p/>
   * Example usage in controller:
   * <pre>   @RequestMapping
   *   public void serve(HttpServletRequest request) {
   *     MetadataFormat mf = MetadataFormat.getFromRequest(request);
   *   }
   * </pre>
   *
   * @param request the request
   * @return the metadata format to use
   */
  public static MetadataFormat getFromRequest(HttpServletRequest request) {
    return getFromAcceptHeader(request.getHeader(ACCEPT_HEADER_NAME));
  }

  /**
   * Provide a metadata format for a set of headers. Match the content types listed in the {@code Accept} header to the
   * first available supported format, or return the default format if none of the formats listed is supported. If there
   * is no {@code Accept} header, return the default format.
   * <p/>
   * In general, controllers should call this method only if they already receive an {@code HttpHeaders} object from
   * Spring for some other reason. If you access the headers only to get the metadata format, prefer {@link
   * #getFromAcceptHeader(String)}.
   * <p/>
   * Example usage in controller:
   * <pre>   @RequestMapping
   *   public void serve(@RequestHeader HttpHeaders headers) {
   *     MetadataFormat mf = getFromHeaders(headers);
   *   }
   * </pre>
   *
   * @param headers the headers from a request
   * @return the metadata format to use
   */
  public static MetadataFormat getFromHeaders(HttpHeaders headers) {
    List<String> values = headers.get(ACCEPT_HEADER_NAME);
    if (values == null || values.isEmpty()) return DEFAULT;
    return getFromAcceptHeader(values.get(0));
  }

}
