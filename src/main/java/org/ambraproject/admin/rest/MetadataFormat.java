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

package org.ambraproject.admin.rest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import org.ambraproject.admin.RestClientException;
import org.springframework.http.HttpStatus;

/**
 * A format for entity metadata, that can be sent to the client in response to a GET request.
 */
public enum MetadataFormat {
  JSON("json"); // singleton only for now; more values can be added as more formats are supported

  private final String parameterValue;

  private MetadataFormat(String parameterValue) {
    this.parameterValue = parameterValue;
  }

  private static final MetadataFormat DEFAULT = JSON;

  private static final ImmutableMap<String, MetadataFormat> BY_EXTENSION = Maps.uniqueIndex(Iterators.forArray(values()),
      new Function<MetadataFormat, String>() {
        @Override
        public String apply(MetadataFormat input) {
          return input.parameterValue.toLowerCase();
        }
      });

  /**
   * Return the metadata format named by a request parameter, or the default if no parameter was provided.
   *
   * @param parameter  a parameter string naming a metadata format, or {@code null} if the client omitted the parameter
   * @param useDefault {@code true} to supply the default format if {@code parameter == null}; {@code false} to return
   *                   null if {@code parameter == null}
   * @return the metadata format object, or the default or null according to {@code useDefault}
   * @throws RestClientException if the parameter is unrecognized
   */
  public static MetadataFormat getFromParameter(String parameter, boolean useDefault) {
    if (parameter == null) {
      return useDefault ? DEFAULT : null;
    }
    MetadataFormat metadataFormat = BY_EXTENSION.get(parameter.toLowerCase());
    if (metadataFormat == null) {
      String message = String.format("Invalid parameter for metadata format: \"%s\". Valid choices are: %s",
          parameter, BY_EXTENSION.keySet().toString());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
    return metadataFormat;
  }

}
