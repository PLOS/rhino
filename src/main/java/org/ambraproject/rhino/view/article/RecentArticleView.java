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

  public ArticleIdentity getDoi() {
    return doi;
  }

  public String getTitle() {
    return title;
  }

  public Date getDate() {
    return date;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RecentArticleView that = (RecentArticleView) o;

    if (!date.equals(that.date)) return false;
    if (!doi.equals(that.doi)) return false;
    if (!title.equals(that.title)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = doi.hashCode();
    result = 31 * result + title.hashCode();
    result = 31 * result + date.hashCode();
    return result;
  }
}
