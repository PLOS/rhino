package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.plos.crepo.model.RepoObject;

import java.util.List;
import java.util.Objects;

class ArticlePackage {

  private final ScholarlyWorkInput articleWork;
  private final ImmutableList<ScholarlyWorkInput> allWorks;
  private final ImmutableList<RepoObject> archivalFiles;

  ArticlePackage(ScholarlyWorkInput articleWork, List<ScholarlyWorkInput> assetWorks, List<RepoObject> archivalFiles) {
    this.articleWork = Objects.requireNonNull(articleWork);
    this.allWorks = ImmutableList.<ScholarlyWorkInput>builder()
        .add(articleWork).addAll(assetWorks).build();
    this.archivalFiles = ImmutableList.copyOf(archivalFiles);
  }

  public DoiBasedIdentity getDoi() {
    return articleWork.getDoi();
  }

  public ImmutableList<ScholarlyWorkInput> getAllWorks() {
    return allWorks;
  }

  public ImmutableList<RepoObject> getArchivalFiles() {
    return archivalFiles;
  }

}
