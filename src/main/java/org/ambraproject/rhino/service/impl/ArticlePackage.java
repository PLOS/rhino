package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.plos.crepo.model.input.RepoObjectInput;

import java.util.List;
import java.util.Objects;

class ArticlePackage {

  private final ArticleItemInput articleWork;
  private final ImmutableList<ArticleItemInput> allWorks;
  private final ImmutableList<RepoObjectInput> archivalFiles;

  ArticlePackage(ArticleItemInput articleWork, List<ArticleItemInput> assetWorks, List<RepoObjectInput> archivalFiles) {
    this.articleWork = Objects.requireNonNull(articleWork);
    this.allWorks = ImmutableList.<ArticleItemInput>builder()
        .add(articleWork).addAll(assetWorks).build();
    this.archivalFiles = ImmutableList.copyOf(archivalFiles);
  }

  public DoiBasedIdentity getDoi() {
    return articleWork.getDoi();
  }

  public ImmutableList<ArticleItemInput> getAllWorks() {
    return allWorks;
  }

  public ImmutableList<RepoObjectInput> getArchivalFiles() {
    return archivalFiles;
  }

}
