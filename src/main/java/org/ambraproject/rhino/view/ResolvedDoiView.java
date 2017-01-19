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

package org.ambraproject.rhino.view;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.view.article.ArticleOverview;
import org.ambraproject.rhino.view.journal.JournalOutputView;

import java.util.Objects;

public class ResolvedDoiView {

  public static enum DoiWorkType {
    ARTICLE, ASSET, VOLUME, ISSUE, COMMENT;
  }

  private static final ImmutableSet<DoiWorkType> BELONGS_TO_ARTICLE = Sets.immutableEnumSet(
      DoiWorkType.ARTICLE, DoiWorkType.ASSET, DoiWorkType.COMMENT);

  private final String doi;
  private final String type;
  private final JournalOutputView journal; // nullable
  private final ArticleOverview article; // nullable

  private ResolvedDoiView(Doi doi, DoiWorkType type, JournalOutputView journal, ArticleOverview article) {
    Preconditions.checkArgument((journal == null) != (article == null));
    this.doi = doi.getName();
    this.type = type.name().toLowerCase();
    this.journal = journal;
    this.article = article;
  }

  public static ResolvedDoiView create(Doi doi, DoiWorkType type, Journal journal) {
    Preconditions.checkArgument(!BELONGS_TO_ARTICLE.contains(type));
    return new ResolvedDoiView(doi, type, JournalOutputView.getView(journal), null);
  }

  public static ResolvedDoiView createForArticle(Doi doi, DoiWorkType type, ArticleOverview article) {
    Preconditions.checkArgument(BELONGS_TO_ARTICLE.contains(type));
    return new ResolvedDoiView(doi, type, null, Objects.requireNonNull(article));
  }

}
