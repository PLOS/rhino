package org.ambraproject.rhino.model.article;

import java.time.LocalDate;

public class ArticleCustomMetadata {

  private final LocalDate revisionDate;
  private final String publicationStage;

  private ArticleCustomMetadata(Builder builder) {
    this.revisionDate = builder.revisionDate;
    this.publicationStage = builder.publicationStage;
  }

  public LocalDate getRevisionDate() {
    return revisionDate;
  }

  public String getPublicationStage() {
    return publicationStage;
  }


  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private LocalDate revisionDate;
    private String publicationStage;

    private Builder() {
    }

    public Builder setRevisionDate(LocalDate revisionDate) {
      this.revisionDate = revisionDate;
      return this;
    }

    public Builder setPublicationStage(String publicationStage) {
      this.publicationStage = publicationStage;
      return this;
    }

    public ArticleCustomMetadata build() {
      return new ArticleCustomMetadata(this);
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleCustomMetadata that = (ArticleCustomMetadata) o;

    if (revisionDate != null ? !revisionDate.equals(that.revisionDate) : that.revisionDate != null) return false;
    return publicationStage != null ? publicationStage.equals(that.publicationStage) : that.publicationStage == null;

  }

  @Override
  public int hashCode() {
    int result = revisionDate != null ? revisionDate.hashCode() : 0;
    result = 31 * result + (publicationStage != null ? publicationStage.hashCode() : 0);
    return result;
  }
}
