package org.ambraproject.rhino.identity;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Information that uniquely identifies an {@link org.ambraproject.models.ArticleList} entity.
 */
public final class ArticleListIdentity {

  private final String listType; // nullable in order to play nice with Gson; always Optional<String> in public
  private final String journalKey; // non-null
  private final String listCode; // non-null

  public ArticleListIdentity(Optional<String> listType, String journalKey, String listCode) {
    this.listType = listType.isPresent() ? validate(listType.get()) : null;
    this.journalKey = validate(journalKey);
    this.listCode = validate(listCode);
  }

  private static final CharMatcher INVALID_CHARACTERS = CharMatcher.WHITESPACE.or(CharMatcher.is('/'));

  private static String validate(String token) {
    Preconditions.checkArgument(!token.isEmpty());
    Preconditions.checkArgument(INVALID_CHARACTERS.matchesNoneOf(token));
    return token;
  }

  public Optional<String> getListType() {
    return Optional.fromNullable(listType);
  }

  public String getJournalKey() {
    return journalKey;
  }

  public String getListCode() {
    return listCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleListIdentity that = (ArticleListIdentity) o;

    if (listType != null ? !listType.equals(that.listType) : that.listType != null) return false;
    if (!journalKey.equals(that.journalKey)) return false;
    if (!listCode.equals(that.listCode)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = listType != null ? listType.hashCode() : 0;
    result = 31 * result + journalKey.hashCode();
    result = 31 * result + listCode.hashCode();
    return result;
  }

}
