package org.ambraproject.rhino.rest;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.identity.Doi;

import java.util.Objects;

/**
 * An identifier used by the client to indicate an article or article item.
 * <p>
 * Resolves into an {@link org.ambraproject.rhino.identity.ArticleIdentifier}, {@link
 * org.ambraproject.rhino.identity.ArticleIngestionIdentifier}, {@link org.ambraproject.rhino.identity.ArticleRevisionIdentifier},
 * or {@link org.ambraproject.rhino.identity.ArticleItemIdentifier}.
 */
public final class ClientItemId {

  public static enum NumberType {INGESTION, REVISION}

  private final Doi doi;
  private final Integer number;
  private final NumberType numberType;

  ClientItemId(Doi doi, Integer number, NumberType numberType) {
    Preconditions.checkArgument((number == null) == (numberType == null));
    this.doi = Objects.requireNonNull(doi);
    this.number = number;
    this.numberType = numberType;
  }

  /**
   * @return the DOI of the requested article or item
   */
  public Doi getDoi() {
    return doi;
  }

  /**
   * @return indicates whether the client requested by ingestion, by revision, or (if null) the latest
   */
  public NumberType getNumberType() {
    return numberType;
  }

  /**
   * @return the number of the requested ingestion or revision  (null means the latest was requested)
   * @see #getNumberType()
   */
  public Integer getNumber() {
    return number;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ClientItemId that = (ClientItemId) o;
    if (!doi.equals(that.doi)) return false;
    if (number != null ? !number.equals(that.number) : that.number != null) return false;
    return numberType == that.numberType;
  }

  @Override
  public int hashCode() {
    int result = doi.hashCode();
    result = 31 * result + (number != null ? number.hashCode() : 0);
    result = 31 * result + (numberType != null ? numberType.hashCode() : 0);
    return result;
  }
}
