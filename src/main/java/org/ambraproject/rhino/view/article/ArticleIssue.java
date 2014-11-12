package org.ambraproject.rhino.view.article;

import com.google.common.base.Preconditions;
import org.ambraproject.models.Issue;
import org.ambraproject.models.Journal;
import org.ambraproject.models.Volume;

/**
 * Wrapper class for Issue which includes parent Volume and Journal objects
 */
public class ArticleIssue {

  private final Issue issue;
  private final Volume parentVolume;
  private final Journal parentJournal;

  public ArticleIssue(Issue issue, Volume parentVolume, Journal parentJournal){
    this.issue = Preconditions.checkNotNull(issue);
    this.parentVolume = Preconditions.checkNotNull(parentVolume);
    this.parentJournal = Preconditions.checkNotNull(parentJournal);
  }

  public Issue getIssue() {
    return issue;
  }

  public Volume getParentVolume() {
    return parentVolume;
  }

  public Journal getParentJournal() {
    return parentJournal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleIssue that = (ArticleIssue) o;

    if (!issue.equals(that.issue)) return false;
    if (!parentJournal.equals(that.parentJournal)) return false;
    if (!parentVolume.equals(that.parentVolume)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = issue.hashCode();
    result = 31 * result + parentVolume.hashCode();
    result = 31 * result + parentJournal.hashCode();
    return result;
  }
}
