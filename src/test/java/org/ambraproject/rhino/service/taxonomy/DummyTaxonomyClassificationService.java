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

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.model.ArticleCategoryAssignment;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Category;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class DummyTaxonomyClassificationService implements TaxonomyClassificationService {

  public static final ImmutableList<WeightedTerm> DUMMY_DATA = ImmutableList.<WeightedTerm>builder()
      .add(new WeightedTerm("/TopLevel1/term1", 5))
      .add(new WeightedTerm("/TopLevel2/term2", 10))
      .build();

  @Override
  public List<WeightedTerm> classifyArticle(Article article, Document articleXml) {
    return DUMMY_DATA;
  }

  @Override
  public List<String> getRawTerms(Document articleXml, Article article,
                                  boolean isTextRequired) throws IOException {
    if (isTextRequired) {
      return ImmutableList.of("dummy text sent to MAIstro", "dummy raw term");
    }
    return ImmutableList.of("dummy raw term");
  }

  @Override
  public void populateCategories(ArticleRevision revision) {

  }

  @Override
  public Collection<ArticleCategoryAssignment> getAssignmentsForArticle(Article article) {
    return null;
  }

  @Override
  public Collection<Category> getArticleCategoriesWithTerm(Article article, String term) {
    return null;
  }
}
