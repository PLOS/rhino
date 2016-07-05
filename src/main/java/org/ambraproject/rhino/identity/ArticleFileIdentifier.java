package org.ambraproject.rhino.identity;

import java.util.Objects;

/**
 * An identifier for a file that is part of the corpus.
 * <p>
 * Only files with a non-null parent article item may be identified this way. (Other files exist only to be repacked.)
 */
public final class ArticleFileIdentifier {

  private final ArticleItemIdentifier itemIdentifier;
  private final String fileType;

  private ArticleFileIdentifier(ArticleItemIdentifier itemIdentifier, String fileType) {
    this.itemIdentifier = Objects.requireNonNull(itemIdentifier);
    this.fileType = Objects.requireNonNull(fileType);
  }

  public static ArticleFileIdentifier create(ArticleItemIdentifier itemIdentifier, String fileType) {
    return new ArticleFileIdentifier(itemIdentifier, fileType);
  }

  public static ArticleFileIdentifier create(Doi doi, int ingestionNumber, String fileType) {
    return create(ArticleItemIdentifier.create(doi, ingestionNumber), fileType);
  }

  public static ArticleFileIdentifier create(String doi, int ingestionNumber, String fileType) {
    return create(ArticleItemIdentifier.create(doi, ingestionNumber), fileType);
  }

  public ArticleItemIdentifier getItemIdentifier() {
    return itemIdentifier;
  }

  public String getFileType() {
    return fileType;
  }

  @Override
  public String toString() {
    return "ArticleFileIdentifier{" +
        "itemIdentifier=" + itemIdentifier +
        ", fileType='" + fileType + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleFileIdentifier that = (ArticleFileIdentifier) o;

    if (!itemIdentifier.equals(that.itemIdentifier)) return false;
    return fileType.equals(that.fileType);

  }

  @Override
  public int hashCode() {
    int result = itemIdentifier.hashCode();
    result = 31 * result + fileType.hashCode();
    return result;
  }
}
