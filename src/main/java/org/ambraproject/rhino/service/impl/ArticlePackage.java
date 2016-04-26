package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.DoiBasedIdentity;

import java.util.List;
import java.util.Objects;

class ArticlePackage {

  private final ScholarlyWork articleWork;
  private final ImmutableList<ScholarlyWork> assetWorks;

  ArticlePackage(ScholarlyWork articleWork, List<ScholarlyWork> assetWorks) {
    this.articleWork = Objects.requireNonNull(articleWork);
    this.assetWorks = ImmutableList.copyOf(assetWorks);
  }

  public DoiBasedIdentity getDoi() {
    return articleWork.getDoi();
  }

  public ScholarlyWork getArticleWork() {
    return articleWork;
  }

  public ImmutableList<ScholarlyWork> getAssetWorks() {
    return assetWorks;
  }

}
