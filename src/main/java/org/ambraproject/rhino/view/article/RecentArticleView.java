package org.ambraproject.rhino.view.article;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;

import java.util.Date;

public class RecentArticleView {

  private final ArticleIdentity doi;
  private final String title;
  private final Date date;

  public RecentArticleView(ArticleIdentity article, String title, Date date) {
    this.doi = Preconditions.checkNotNull(article);
    this.title = Preconditions.checkNotNull(title);
    this.date = Preconditions.checkNotNull(date);
  }

}
