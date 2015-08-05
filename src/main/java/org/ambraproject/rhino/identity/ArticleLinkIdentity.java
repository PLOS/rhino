package org.ambraproject.rhino.identity;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

/**
 * Information that uniquely identifies an {@link org.ambraproject.rhino.model.ArticleLink} entity.
 */
public final class ArticleLinkIdentity {

  private final String linkType;
  private final String journalKey;
  private final String target;

  public ArticleLinkIdentity(String linkType, String journalKey, String target) {
    this.linkType = validate(linkType);
    this.journalKey = validate(journalKey);
    this.target = validate(target);
  }

  private static final CharMatcher INVALID_CHARACTERS = CharMatcher.WHITESPACE.or(CharMatcher.is('/'));

  private static String validate(String token) {
    Preconditions.checkArgument(!token.isEmpty());
    Preconditions.checkArgument(INVALID_CHARACTERS.matchesNoneOf(token));
    return token;
  }

  public String getLinkType() {
    return linkType;
  }

  public String getJournalKey() {
    return journalKey;
  }

  public String getTarget() {
    return target;
  }

  @Override
  public String toString() {
    return linkType + '/' + journalKey + '/' + target;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleLinkIdentity that = (ArticleLinkIdentity) o;

    if (!linkType.equals(that.linkType)) return false;
    if (!journalKey.equals(that.journalKey)) return false;
    if (!target.equals(that.target)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = linkType.hashCode();
    result = 31 * result + journalKey.hashCode();
    result = 31 * result + target.hashCode();
    return result;
  }

}
