/*
 * Copyright (c) 2006-2014 by Public Library of Science
 *
 *    http://plos.org
 *    http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.config;

import java.net.URI;

/**
 * Interface that represents configurable values that are only known at server startup time.
 */
public interface RuntimeConfiguration {

  /**
   * determine if json should be formatted or not
   *
   * @return false when formatting is turned off
   */
  boolean prettyPrintJson();

  /**
   * Return a URI at which to connect to a content repository API. If the URI has the {@code file} scheme (e.g., {@code
   * file:///home/me/myDummyRepo/}), will simulate a "dev mode" content repository using files in that directory.
   *
   * @return the content repository URI
   */
  URI getContentRepoAddress();

}
