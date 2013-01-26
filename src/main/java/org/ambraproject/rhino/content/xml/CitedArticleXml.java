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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.models.CitedArticleAuthor;
import org.ambraproject.models.CitedArticleEditor;
import org.ambraproject.models.CitedArticlePerson;
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

  protected CitedArticleXml(Node xml) {
    super(xml);
  }

  @Override
  public CitedArticle build(CitedArticle citation) throws XmlContentException {
    setTypeAndJournal(citation);
    citation.setTitle(buildTitle());
    String volume = readString("volume");
    citation.setVolume(volume);
    Integer volumeNumber = parseVolumeNumber(volume);
    if (volumeNumber != null) {
      citation.setVolumeNumber(volumeNumber);
    }
    citation.setIssue(readString("issue"));
    citation.setPublisherLocation(readString("publisher-loc"));
    citation.setPublisherName(readString("publisher-name"));
    citation.setNote(readString("comment"));

    String displayYear = readString("year");
    citation.setDisplayYear(displayYear);
    citation.setYear(parseYear(displayYear));
    citation.setMonth(readString("month"));
    citation.setDay(readString("day"));

    citation.setPages(buildPages());
    citation.seteLocationID(buildELocationId());

    citation.setAuthors(readAuthors(readNodeList("person-group[@person-group-type=\"author\"]/name")));
    citation.setEditors(readEditors(readNodeList("person-group[@person-group-type=\"editor\"]/name")));

    // TODO Finish implementing

    return citation;
  }

  /**
   * Sets the citationType and journal properties of a CitedArticle appropriately based on the XML.
   */
  private void setTypeAndJournal(CitedArticle citation) {
    String type = readString("@publication-type");
    if (Strings.isNullOrEmpty(type)) {
      return;
    }

    // pmc2obj-v3.xslt lines 730-739
    if ("journal".equals(type)) {
      type = "Article";
      citation.setJournal(readString("source[1]"));
    } else if ("book".equals(type)) {
      type = "Book";
    } else {
      type = "Misc";
    }
    citation.setCitationType("http://purl.org/net/nknouf/ns/bibtex#" + type);
  }

  /**
   * @return the value of the title property, retrieved from the XML
   */
  private String buildTitle() {

    // pmc2obj-v3.xslt lines 348-350
    Node titleNode = readNode("article-title");
    if (titleNode == null) {
      titleNode = readNode("source");
    }
    if (titleNode != null) {
      StringBuilder title = new StringBuilder();
      buildTextWithMarkup(title, titleNode);
      return title.toString();
    } else {
      return "";
    }
  }

  /**
   * @return the value of the pages property, retrieved from the XML
   */
  private String buildPages() {

    // pmc2obj-v3.xslt lines 353-356
    String range = readString("page-range");
    if (!Strings.isNullOrEmpty(range)) {
      return range;
    } else {
      String fpage = readString("fpage");
      String lpage = readString("lpage");
      if (!Strings.isNullOrEmpty(lpage)) {
        return fpage + "-" + lpage;
      } else {
        return fpage;
      }
    }
  }

  /**
   * @return the value of the eLocationId property, retrieved from the XML
   */
  private String buildELocationId() {
    List<String> parts = readTextList("elocation-id | fpage");
    return Joiner.on(" ").skipNulls().join(parts);
  }

  private static final Pattern VOL_NUM_RE = Pattern.compile("(\\d{1,})");

  @VisibleForTesting
  static Integer parseVolumeNumber(String volume) {
    if (Strings.isNullOrEmpty(volume)) {
      return null;
    }

    // pmc2obj-v3.xslt lines 742-753.  Note that there is currently a bug in this
    // function, however--it returns 801 for the string "80(1)", instead of 80!
    Matcher match = VOL_NUM_RE.matcher(volume);
    if (match.find()) {
      return Integer.parseInt(match.group());
    } else {
      return null;
    }
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

  private List<CitedArticleAuthor> readAuthors(List<Node> authorNodes) throws XmlContentException {
    List<CitedArticleAuthor> authors = Lists.newArrayListWithCapacity(authorNodes.size());
    for (Node authorNode : authorNodes) {
      CitedArticleAuthor author = parsePersonName(authorNode).copyTo(new CitedArticleAuthor());
      author = emptySuffixToNull(author);
      authors.add(author);
    }
    return authors;
  }

  private List<CitedArticleEditor> readEditors(List<Node> editorNodes) throws XmlContentException {
    List<CitedArticleEditor> editors = Lists.newArrayListWithCapacity(editorNodes.size());
    for (Node editorNode : editorNodes) {
      CitedArticleEditor editor = parsePersonName(editorNode).copyTo(new CitedArticleEditor());
      editor = emptySuffixToNull(editor);
      editors.add(editor);
    }
    return editors;
  }

  /**
   * Reproducing an Ambra quirk: {@link CitedArticlePerson} objects have a null suffix where {@link
   * org.ambraproject.models.ArticlePerson} objects have an empty string.
   * <p/>
   * It would be better to always prefer empty strings over null, but we would need to be certain that it wouldn't
   * introduce bugs to Ambra before we change behavior.
   *
   * @param person the object to modify
   * @param <T>    the CitedArticlePerson type
   * @return the same object, with a null suffix if its suffix was empty
   */
  private static <T extends CitedArticlePerson> T emptySuffixToNull(T person) {
    if (person.getSuffix().isEmpty()) {
      person.setSuffix(null);
    }
    return person;
  }

}
