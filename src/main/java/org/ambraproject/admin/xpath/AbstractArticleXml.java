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

package org.ambraproject.admin.xpath;

import org.ambraproject.admin.util.PersonName;
import org.ambraproject.models.AmbraEntity;
import org.w3c.dom.Node;

/**
 * A holder for a piece (node or document) of NLM-format XML, which can be built into an entity.
 *
 * @param <T> the type of entity that can be build from this XML element
 */
public abstract class AbstractArticleXml<T extends AmbraEntity> extends XmlToObject<T> {

  public AbstractArticleXml(Node xml) {
    super(xml);
  }

  // Legal values for the "name-style" attribute of a <name> node
  private static final String WESTERN_NAME_STYLE = "western";
  private static final String EASTERN_NAME_STYLE = "eastern";

  protected PersonName parsePersonName(Node nameNode)
      throws XmlContentException {
    String nameStyle = readString("@name-style", nameNode);
    String surname = readString("surname", nameNode);
    String givenName = readString("given-names", nameNode);
    String suffix = readString("suffix", nameNode);

    if (surname == null) {
      throw new XmlContentException("Required surname is omitted");
    }
    if (givenName == null) {
      throw new XmlContentException("Required given name is omitted");
    }

    String fullName;
    if (WESTERN_NAME_STYLE.equals(nameStyle)) {
      fullName = buildFullName(givenName, surname, suffix);
    } else if (EASTERN_NAME_STYLE.equals(nameStyle)) {
      fullName = buildFullName(surname, givenName, suffix);
    } else {
      throw new XmlContentException("Invalid name-style: " + nameStyle);
    }

    return new PersonName(fullName, givenName, surname, suffix);
  }

  /*
   * Preconditions: firstName and lastName are both non-null and non-empty; suffix may be null
   */
  private static String buildFullName(String firstName, String lastName, String suffix) {
    boolean hasSuffix = (suffix != null);
    int expectedLength = 2 + firstName.length() + lastName.length() + (hasSuffix ? suffix.length() : -1);
    StringBuilder name = new StringBuilder(expectedLength);
    name.append(firstName).append(' ').append(lastName);
    if (hasSuffix) {
      name.append(' ').append(suffix);
    }
    return name.toString();
  }


}

