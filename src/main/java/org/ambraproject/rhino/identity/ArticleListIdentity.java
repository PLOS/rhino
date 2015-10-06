package org.ambraproject.rhino.identity;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

/**
 * Information that uniquely identifies an {@link org.ambraproject.models.ArticleList} entity.
 */
public final class ArticleListIdentity {

  private final String listType;
  private final String journalKey;
  private final String listCode;

  public ArticleListIdentity(String listType, String journalKey, String listCode) {
    this.listType = validate(listType);
    this.journalKey = validate(journalKey);
    this.listCode = validate(listCode);
  }

  private static final CharMatcher INVALID_CHARACTERS = CharMatcher.WHITESPACE.or(CharMatcher.is('/'));

  private static String validate(String token) {
    Preconditions.checkArgument(!token.isEmpty());
    Preconditions.checkArgument(INVALID_CHARACTERS.matchesNoneOf(token));
    return token;
  }

  public String getListType() {
    return listType;
  }

  public String getJournalKey() {
    return journalKey;
  }

  public String getListCode() {
    return listCode;
  }

  @Override
  public String toString() {
    return listType + "/" + journalKey + "/" + listCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleListIdentity that = (ArticleListIdentity) o;

    if (!listType.equals(that.listType)) return false;
    if (!journalKey.equals(that.journalKey)) return false;
    if (!listCode.equals(that.listCode)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = listType.hashCode();
    result = 31 * result + journalKey.hashCode();
    result = 31 * result + listCode.hashCode();
    return result;
  }

}
