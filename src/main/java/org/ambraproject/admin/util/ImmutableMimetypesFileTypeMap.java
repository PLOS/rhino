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

package org.ambraproject.admin.util;

import javax.activation.MimetypesFileTypeMap;
import java.io.IOException;
import java.io.InputStream;

public class ImmutableMimetypesFileTypeMap extends MimetypesFileTypeMap {

  public ImmutableMimetypesFileTypeMap() {
    super();
  }

  public ImmutableMimetypesFileTypeMap(InputStream inputStream) {
    super(inputStream);
  }

  public ImmutableMimetypesFileTypeMap(String s) throws IOException {
    super(s);
  }

  /**
   * @deprecated This object is immutable.
   */
  @Deprecated
  @Override
  public final synchronized void addMimeTypes(String s) {
    throw new UnsupportedOperationException();
  }

}
