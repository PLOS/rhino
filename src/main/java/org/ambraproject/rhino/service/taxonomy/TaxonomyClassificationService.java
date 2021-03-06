/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.service.taxonomy;

import org.ambraproject.rhino.model.ArticleCategoryAssignment;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Category;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * Service for assigning taxonomic terms to an article.
 */
public interface TaxonomyClassificationService {

  /**
   * Classify an article from its xml.
   *
   * @param article article object from Ambra
   * @param articleXml the article xml
   * @return a map of categories to which the article belongs. Each entry should use <code>/</code>s to delimit subject
   * hierarchy.  Categories are returned in descending order of the strength of the match paired with the strength
   * value
   * @throws TaxonomyRemoteServiceNotConfiguredException if a remote service is required but not configured
   */
  public List<WeightedTerm> classifyArticle(Article article, Document articleXml);

  /**
   * Queries the MAI server for taxonomic terms for a given article, and returns a list of the raw results.
   * If {@param isTextRequired} is true, it will also return the text that is sent to taxonomy server.
   *
   * @param articleXml DOM of the article to categorize
   * @param article article object from Ambra
   * @param isTextRequired if true, returns the text sent to the taxonomy server for classification
   * @return List of results from the server.  This will consist of raw XML fragments, and include things like counts
   * that we don't currently store in mysql. The first element of the list will be the text sent to the
   * server if  {@param isTextRequired} is true.
   * @throws IOException
   */
  public List<String> getRawTerms(Document articleXml, Article article,
                                  boolean isTextRequired) throws IOException;

  /**
   * Populates article category information by making a call to the taxonomy server. Will not throw
   * an exception if we cannot communicate or get results from the taxonomy server. Will not
   * request categories for amendments.
   *
   * @param revision the article revision model instance
   */
  public void populateCategories(ArticleRevision revision);

  public Collection<ArticleCategoryAssignment> getAssignmentsForArticle(Article article);

  public Collection<Category> getArticleCategoriesWithTerm(Article article, String term);

}
