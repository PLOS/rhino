package org.ambraproject.rhino.identity;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;

/**
 * Information that uniquely identifies an {@link org.ambraproject.models.ArticleList} entity.
 */
public final class ArticleListIdentity {

  private final String type;
  private final String journalKey;
  private final String key;

  public ArticleListIdentity(String type, String journalKey, String key) {
    this.type = validate(type);
    this.journalKey = validate(journalKey);
    this.key = validate(key);
  }

  private static final CharMatcher INVALID_CHARACTERS = CharMatcher.WHITESPACE.or(CharMatcher.is('/'));

  private static String validate(String token) {
    Preconditions.checkArgument(!token.isEmpty());
    Preconditions.checkArgument(INVALID_CHARACTERS.matchesNoneOf(token));
    return token;
  }

  public String getType() {
    return type;
  }

  public String getJournalKey() {
    return journalKey;
  }

  public String getKey() {
    return key;
  }

  @Override
  public String toString() {
    return type + "/" + journalKey + "/" + key;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleListIdentity that = (ArticleListIdentity) o;

    if (!type.equals(that.type)) return false;
    if (!journalKey.equals(that.journalKey)) return false;
    if (!key.equals(that.key)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + journalKey.hashCode();
    result = 31 * result + key.hashCode();
    return result;
  }

}
