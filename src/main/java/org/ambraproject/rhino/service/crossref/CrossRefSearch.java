/*
 * Copyright (c) 2006-2014 by Public Library of Science
 *
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambraproject.rhino.service.crossref;

import com.google.common.collect.ImmutableList;
import org.ambraproject.util.XPathUtil;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathException;
import java.util.ArrayList;
import java.util.List;

/**
 * Used for building up cross ref search terms and then storing the query results
 */
class CrossRefSearch {
  private final ImmutableList<CitedArticleName> names;
  private final String title;
  private final String source;
  private final String volume;
  private final String issue;
  private final String fPage;
  private final String lPage;
  private final String elocationID;
  private final String year;

  //Label is used for storage of the citation once crossref gives us a DOI
  private final String label;
  private final long originalOrder;

  private final class CitedArticleName {
    private final String surName;
    private final String givenName;
    private final String collab;

    public CitedArticleName(String surName, String givenName, String collab) {
      this.surName = surName;
      this.givenName = givenName;
      this.collab = collab;
    }
  }

  public CrossRefSearch(Node node, long originalOrder) throws XPathException {
    XPathUtil xPathUtil = new XPathUtil();

    List<CrossRefSearch.CitedArticleName> names = new ArrayList<>();

    NodeList nameNodes = xPathUtil.selectNodes(node, ".//name");

    for (int a = 0; a < nameNodes.getLength(); a++) {
      Node nameNode = nameNodes.item(a);
      CitedArticleName citedArticleName = new CitedArticleName(
          xPathUtil.selectString(nameNode, "surname"),
          xPathUtil.selectString(nameNode, "given-names"),
          null);

      names.add(citedArticleName);
    }

    NodeList collabNodes = xPathUtil.selectNodes(node, ".//collab");
    for (int a = 0; a < collabNodes.getLength(); a++) {
      CitedArticleName citedArticleName = new CitedArticleName(null, null,
          xPathUtil.selectString(collabNodes.item(0), "."));

      names.add(citedArticleName);
    }

    this.names = ImmutableList.copyOf(names);

    this.title = xPathUtil.selectString(node, ".//article-title");
    this.source = xPathUtil.selectString(node, ".//source");
    this.volume = xPathUtil.selectString(node, ".//volume");
    this.issue = xPathUtil.selectString(node, ".//issue");
    this.fPage = xPathUtil.selectString(node, ".//fpage");
    this.lPage = xPathUtil.selectString(node, ".//lpage");
    this.elocationID = xPathUtil.selectString(node, ".//elocation-id");
    this.year = xPathUtil.selectString(node, ".//year");
    this.label = xPathUtil.selectString(node, ".//label");
    this.originalOrder = originalOrder;
  }

  /**
   * Build search query to pass to the crossref search API
   *
   * @return a query string
   */
  public String buildQuery() {
    StringBuilder sb = new StringBuilder();

    for (CitedArticleName name : this.names) {
      String fullName = "";

      if (StringUtils.isNotBlank(name.surName) && StringUtils.isNotBlank(name.givenName)) {
        if (StringUtils.isAllUpperCase(name.givenName.replaceAll("[^A-Za-z0-9]", ""))) {
          for (Character c : name.givenName.toCharArray()) {
            fullName = fullName + c + ". ";
          }
          fullName = fullName + name.surName;
        } else {
          fullName = name.givenName + " " + name.surName;
        }
      } else {
        if (StringUtils.isNotBlank(name.surName)) {
          fullName = name.surName;
        }

        if (StringUtils.isNotBlank(name.collab)) {
          fullName = name.collab;
        }
      }

      if (StringUtils.isNotBlank(fullName)) {
        if (sb.length() > 0) {
          sb.append(", ");
        }

        sb.append(fullName);
      }
    }


    if (StringUtils.isNotBlank(this.title)) {
      sb.append(", \"").append(this.title).append("\"");
    }

    if (StringUtils.isNotBlank(this.source)) {
      sb.append(", ").append(this.source);
    }

    if (StringUtils.isNotBlank(this.volume)) {
      sb.append(", vol. ").append(volume);
    }

    if (StringUtils.isNotBlank(this.issue)) {
      sb.append(", no. ").append(this.issue);
    }

    if (StringUtils.isNotBlank(this.fPage) && StringUtils.isNotBlank(this.lPage)) {
      sb.append(", pp. ").append(this.fPage).append("-").append(this.lPage);
    } else if (StringUtils.isNotBlank(this.fPage)) {
      sb.append(", pp. ").append(this.fPage);
    }

    if (StringUtils.isNotBlank(this.elocationID)) {
      sb.append(", e").append(this.elocationID);
    }

    if (StringUtils.isNotBlank(this.year)) {
      sb.append(", ").append(this.year);
    }

    return sb.toString();
  }

  public String getLabel() {
    return label;
  }

  public long getOriginalOrder() {
    return originalOrder;
  }
}
