package org.ambraproject.rhino.test.casetype;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.test.AssertionFailure;
import org.ambraproject.rhino.test.ExpectedEntity;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;

/**
 * Generated code! See {@code /src/test/python/ingest_test_generation/generate.py}
 */
public class ExpectedArticleAsset extends ExpectedEntity<ArticleAsset> {
  private String doi;
  private String contextElement;
  private String extension;
  private String contentType;
  private String title;
  private String description;
  private long size;

  public ExpectedArticleAsset() {
    super(ArticleAsset.class);
  }

  @Override
  public ImmutableCollection<AssertionFailure<?>> test(ArticleAsset articleAsset) {
    Collection<AssertionFailure<?>> failures = Lists.newArrayList();
    testField(failures, "doi", articleAsset.getDoi(), doi);
    testField(failures, "contextElement", articleAsset.getContextElement(), contextElement);
    testField(failures, "extension", articleAsset.getExtension(), extension);
    testField(failures, "contentType", articleAsset.getContentType(), contentType);
    testField(failures, "title", articleAsset.getTitle(), title);
    testField(failures, "description", articleAsset.getDescription(), description);
    testField(failures, "size", articleAsset.getSize(), size);
    return ImmutableList.copyOf(failures);
  }

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getContextElement() {
    return contextElement;
  }

  public void setContextElement(String contextElement) {
    this.contextElement = contextElement;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
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
