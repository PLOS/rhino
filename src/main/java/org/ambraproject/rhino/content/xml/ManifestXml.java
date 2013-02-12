/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.content.xml;

import org.w3c.dom.Node;

/**
 * Represents the manifest of an article .zip archive.
 * <p/>
 * This class is different from the other classes in this package in that it is not
 * parameterized on a subclass of org.ambraproject.models.AmbraEntity.  This is
 * because there is no persistent representation of a manifest beyond the zip file.
 * We're extending XmlToObject here only to make use of its XPath utilities.
 */
public class ManifestXml extends XmlToObject<Object> {

  /**
   * Constructor.
   *
   * @param xml the XML content of the manifest file
   */
  public ManifestXml(Node xml) {
    super(xml);
  }

  /**
   * We extend this method only because we must to create a concrete subclass.
   * It should never be called.
   *
   * @param obj irrelevant
   * @return never
   * @throws UnsupportedOperationException
   */
  public Object build(Object obj) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return the name of the file in the zip archive that is the XML article
   */
  public String getArticleXml() {
    return readString("//article/@main-entry");
  }
}
