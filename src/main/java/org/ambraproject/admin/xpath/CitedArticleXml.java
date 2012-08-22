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

import com.google.common.collect.Lists;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.models.CitedArticleAuthor;
import org.ambraproject.models.CitedArticleEditor;
import org.w3c.dom.Node;

import java.util.List;

public class CitedArticleXml extends AbstractArticleXml<CitedArticle> {

  protected CitedArticleXml(Node xml) {
    super(xml);
  }

  @Override
  public CitedArticle build(CitedArticle citation) throws XmlContentException {
    citation.setCitationType(readString("@citation-type"));
    citation.setTitle(readString("article-title"));
    citation.setVolume(readString("volume"));
    citation.setIssue(readString("issue"));
    citation.setPublisherLocation(readString("publisher-loc"));
    citation.setPublisherName(readString("publisher-name"));

    citation.setAuthors(readAuthors(readNodeList("person-group[@person-group-type=\"author\"]/name")));
    citation.setEditors(readEditors(readNodeList("person-group[@person-group-type=\"editor\"]/name")));

    // TODO Finish implementing

    return citation;
  }

  private List<CitedArticleAuthor> readAuthors(List<Node> authorNodes) throws XmlContentException {
    if (authorNodes == null) {
      return null;
    }
    List<CitedArticleAuthor> authors = Lists.newArrayListWithCapacity(authorNodes.size());
    for (Node authorNode : authorNodes) {
      CitedArticleAuthor author = parsePersonName(authorNode).copyTo(new CitedArticleAuthor());
      authors.add(author);
    }
    return authors;
  }

  private List<CitedArticleEditor> readEditors(List<Node> editorNodes) throws XmlContentException {
    if (editorNodes == null) {
      return null;
    }
    List<CitedArticleEditor> editors = Lists.newArrayListWithCapacity(editorNodes.size());
    for (Node editorNode : editorNodes) {
      CitedArticleEditor editor = parsePersonName(editorNode).copyTo(new CitedArticleEditor());
      editors.add(editor);
    }
    return editors;
  }

}
