package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.DoiBasedIdentity;

import java.util.List;
import java.util.Objects;

class ArticlePackage {

  private final ScholarlyWorkInput articleWork;
  private final ImmutableList<ScholarlyWorkInput> assetWorks;

  ArticlePackage(ScholarlyWorkInput articleWork, List<ScholarlyWorkInput> assetWorks) {
    this.articleWork = Objects.requireNonNull(articleWork);
    this.assetWorks = ImmutableList.copyOf(assetWorks);
  }

  public DoiBasedIdentity getDoi() {
    return articleWork.getDoi();
  }

  public ScholarlyWorkInput getArticleWork() {
    return articleWork;
  }

  public ImmutableList<ScholarlyWorkInput> getAssetWorks() {
    return assetWorks;
  }

}
