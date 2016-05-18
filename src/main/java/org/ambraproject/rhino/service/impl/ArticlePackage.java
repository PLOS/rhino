package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.plos.crepo.model.RepoObject;

import java.util.List;
import java.util.Objects;

class ArticlePackage {

  private final ScholarlyWorkInput articleWork;
  private final ImmutableList<ScholarlyWorkInput> assetWorks;
  private final ImmutableList<RepoObject> archivalFiles;

  ArticlePackage(ScholarlyWorkInput articleWork, List<ScholarlyWorkInput> assetWorks, List<RepoObject> archivalFiles) {
    this.articleWork = Objects.requireNonNull(articleWork);
    this.assetWorks = ImmutableList.copyOf(assetWorks);
    this.archivalFiles = ImmutableList.copyOf(archivalFiles);
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

  public ImmutableList<RepoObject> getArchivalFiles() {
    return archivalFiles;
  }

}
