package org.ambraproject.rhino.test.casetype;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.test.IngestionTestCase;
import org.ambraproject.rhino.test.IngestionTestCase.AssertionFailure;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;

public class ExpectedArticleAsset extends IngestionTestCase.ExpectedEntity<ArticleAsset> {
  private final String doi;
  private final String contextElement;
  private final String extension;
  private final String contentType;
  private final String title;
  private final String description;
  private final long size;

  private ExpectedArticleAsset(Builder builder) {
    super(ArticleAsset.class);
    this.doi = builder.doi;
    this.contextElement = builder.contextElement;
    this.extension = builder.extension;
    this.contentType = builder.contentType;
    this.title = builder.title;
    this.description = builder.description;
    this.size = builder.size;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Collection<AssertionFailure> test(ArticleAsset articleAsset) {
    Collection<AssertionFailure> failures = Lists.newArrayList();

    String doi = articleAsset.getDoi();
    if (!Objects.equal(doi, this.doi)) {
      failures.add(AssertionFailure.create(ArticleAsset.class, "doi", doi, this.doi));
    }

    String contextElement = articleAsset.getContextElement();
    if (!Objects.equal(contextElement, this.contextElement)) {
      failures.add(AssertionFailure.create(ArticleAsset.class, "contextElement", contextElement, this.contextElement));
    }

    String extension = articleAsset.getExtension();
    if (!Objects.equal(extension, this.extension)) {
      failures.add(AssertionFailure.create(ArticleAsset.class, "extension", extension, this.extension));
    }

    String contentType = articleAsset.getContentType();
    if (!Objects.equal(contentType, this.contentType)) {
      failures.add(AssertionFailure.create(ArticleAsset.class, "contentType", contentType, this.contentType));
    }

    String title = articleAsset.getTitle();
    if (!Objects.equal(title, this.title)) {
      failures.add(AssertionFailure.create(ArticleAsset.class, "title", title, this.title));
    }

    String description = articleAsset.getDescription();
    if (!Objects.equal(description, this.description)) {
      failures.add(AssertionFailure.create(ArticleAsset.class, "description", description, this.description));
    }

    long size = articleAsset.getSize();
    if (size != this.size) {
      failures.add(AssertionFailure.create(ArticleAsset.class, "size", size, this.size));
    }

    return ImmutableList.copyOf(failures);
  }

  public static class Builder {
    private String doi;
    private String contextElement;
    private String extension;
    private String contentType;
    private String title;
    private String description;
    private long size;

    public Builder setDoi(String doi) {
      this.doi = doi;
      return this;
    }

    public Builder setContextElement(String contextElement) {
      this.contextElement = contextElement;
      return this;
    }

    public Builder setExtension(String extension) {
      this.extension = extension;
      return this;
    }

    public Builder setContentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    public Builder setTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setSize(long size) {
      this.size = size;
      return this;
    }

    public ExpectedArticleAsset build() {
      return new ExpectedArticleAsset(this);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != getClass()) return false;
    ExpectedArticleAsset that = (ExpectedArticleAsset) obj;
    if (!Objects.equal(this.doi, that.doi)) return false;
    if (!Objects.equal(this.contextElement, that.contextElement)) return false;
    if (!Objects.equal(this.extension, that.extension)) return false;
    if (!Objects.equal(this.contentType, that.contentType)) return false;
    if (!Objects.equal(this.title, that.title)) return false;
    if (!Objects.equal(this.description, that.description)) return false;
    if (!Objects.equal(this.size, that.size)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int hash = 1;
    hash = prime * hash + ObjectUtils.hashCode(doi);
    hash = prime * hash + ObjectUtils.hashCode(contextElement);
    hash = prime * hash + ObjectUtils.hashCode(extension);
    hash = prime * hash + ObjectUtils.hashCode(contentType);
    hash = prime * hash + ObjectUtils.hashCode(title);
    hash = prime * hash + ObjectUtils.hashCode(description);
    hash = prime * hash + ObjectUtils.hashCode(size);
    return hash;
  }
}
