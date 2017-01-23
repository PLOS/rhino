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

package org.ambraproject.rhino.identity;

import org.ambraproject.rhino.model.ArticleIngestion;

import java.util.Objects;

public final class ArticleIngestionIdentifier {

  private final ArticleIdentifier articleIdentifier;
  private final int ingestionNumber;

  private ArticleIngestionIdentifier(ArticleIdentifier articleIdentifier, int ingestionNumber) {
    this.articleIdentifier = Objects.requireNonNull(articleIdentifier);
    this.ingestionNumber = ingestionNumber;
  }

  public static ArticleIngestionIdentifier create(ArticleIdentifier articleIdentifier, int ingestionNumber) {
    return new ArticleIngestionIdentifier(articleIdentifier, ingestionNumber);
  }

  public static ArticleIngestionIdentifier create(Doi articleIdentifier, int ingestionNumber) {
    return create(ArticleIdentifier.create(articleIdentifier), ingestionNumber);
  }

  public static ArticleIngestionIdentifier create(String articleIdentifier, int ingestionNumber) {
    return create(ArticleIdentifier.create(articleIdentifier), ingestionNumber);
  }

  public static ArticleIngestionIdentifier of(ArticleIngestion ingestion) {
    return create(ArticleIdentifier.create(ingestion.getArticle().getDoi()), ingestion.getIngestionNumber());
  }

  public ArticleIdentifier getArticleIdentifier() {
    return articleIdentifier;
  }

  public int getIngestionNumber() {
    return ingestionNumber;
  }

  // Convenience method for unpacking into persistent form
  public String getDoiName() {
    return articleIdentifier.getDoiName();
  }

  /**
   * @return the identifier for the article item containing this version's manuscript
   */
  public ArticleItemIdentifier getItemFor() {
    return ArticleItemIdentifier.create(articleIdentifier.getDoi(), ingestionNumber);
  }

  @Override
  public String toString() {
    return "ArticleIngestionIdentifier{" +
        "articleIdentifier=" + articleIdentifier +
        ", ingestionNumber=" + ingestionNumber +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleIngestionIdentifier that = (ArticleIngestionIdentifier) o;

    if (ingestionNumber != that.ingestionNumber) return false;
    return articleIdentifier.equals(that.articleIdentifier);

  }

  @Override
  public int hashCode() {
    int result = articleIdentifier.hashCode();
    result = 31 * result + ingestionNumber;
    return result;
  }

}
