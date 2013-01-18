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

package org.ambraproject.rhino.content.xml;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.models.CitedArticleAuthor;
import org.ambraproject.models.CitedArticleEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A holder for an NLM-format XML node that represents an article citation.
 */
public class CitedArticleXml extends AbstractArticleXml<CitedArticle> {

  private static final Logger log = LoggerFactory.getLogger(CitedArticleXml.class);

  /*
   * Possible values for the "citation-type" attribute that indicate that the "source" element is a journal name and
   * should go into the CitedArticle.journal field.
   */
  private static final ImmutableSet<String> JOURNAL_TYPES = ImmutableSet.of("journal", "confproc");

  protected CitedArticleXml(Node xml) {
    super(xml);
  }

  @Override
  public CitedArticle build(CitedArticle citation) throws XmlContentException {
    String citationType = readString("@citation-type");
    citation.setCitationType(citationType);
    if (JOURNAL_TYPES.contains(citationType)) {
      citation.setJournal(readString("source"));
    }

    citation.setTitle(readString("article-title"));
    citation.setVolume(readString("volume"));
    citation.setIssue(readString("issue"));
    citation.setPublisherLocation(readString("publisher-loc"));
    citation.setPublisherName(readString("publisher-name"));
    citation.setNote(readString("comment"));

    String displayYear = readString("year");
    citation.setDisplayYear(displayYear);
    citation.setYear(parseYear(displayYear));
    citation.setMonth(readString("month"));
    citation.setDay(readString("day"));

    citation.setPages(parsePageRange(readString("fpage"), readString("lpage")));

    citation.setAuthors(readAuthors(readNodeList("person-group[@person-group-type=\"author\"]/name")));
    citation.setEditors(readEditors(readNodeList("person-group[@person-group-type=\"editor\"]/name")));

    // TODO Finish implementing

    return citation;
  }

  private Integer parseYear(String displayYear) {
    if (displayYear == null) {
      return null;
    }
    try {
      return Integer.valueOf(displayYear);
    } catch (NumberFormatException e) {
      log.error("Year is not a number: " + displayYear, e);
      return parseYearFallback(displayYear);
      // TODO: Report to client?
    }
  }

  /**
   * As a fallback for parsing the display year into an integer, if there is one uninterrupted sequence of digits,
   * assume that it is the year.  This deals with a bug known from {@code pone.0005723.xml}, where display years have
   * one-letter suffixes (e.g., "2000b").
   * <p/>
   * TODO: Better solution; find out how Admin would handle this
   *
   * @param displayYear the display year given as text in the article XML
   * @return the year as a number, if exactly one sequence of digits is found in the displayYear; else {@code null}
   */
  private Integer parseYearFallback(String displayYear) {
    Matcher matcher = YEAR_FALLBACK.matcher(displayYear);
    if (!matcher.find()) {
      return null; // displayYear contains no sequence of digits
    }
    String displayYearSub = matcher.group();
    if (matcher.find()) {
      return null; // displayYear contains more than one sequence of digits; we don't know which is the year
    }
    return Integer.valueOf(displayYearSub); // YEAR_FALLBACK should guarantee that this parses validly
  }

  private static final Pattern YEAR_FALLBACK = Pattern.compile("\\d+");

  protected String parsePageRange(String firstPage, String lastPage) {
    if (firstPage != null) {
      return (lastPage == null) ? firstPage : (firstPage + '-' + lastPage);
    }
    return null;
  }

  private List<CitedArticleAuthor> readAuthors(List<Node> authorNodes) throws XmlContentException {
    List<CitedArticleAuthor> authors = Lists.newArrayListWithCapacity(authorNodes.size());
    for (Node authorNode : authorNodes) {
      CitedArticleAuthor author = parsePersonName(authorNode).copyTo(new CitedArticleAuthor());
      authors.add(author);
    }
    return authors;
  }

  private List<CitedArticleEditor> readEditors(List<Node> editorNodes) throws XmlContentException {
    List<CitedArticleEditor> editors = Lists.newArrayListWithCapacity(editorNodes.size());
    for (Node editorNode : editorNodes) {
      CitedArticleEditor editor = parsePersonName(editorNode).copyTo(new CitedArticleEditor());
      editors.add(editor);
    }
    return editors;
  }

}
