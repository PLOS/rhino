package org.ambraproject.rhino.view.article;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class ArticleAllAuthorsView  {

  private final ImmutableList<ArticleAuthorView> authors;
  private final ImmutableList<String> authorContributions;
  private final ImmutableList<String> competingInterests;

  public ArticleAllAuthorsView(List<ArticleAuthorView> authors, List<String> authorContributions,
      List<String> competingInterests) {
    this.authorContributions = ImmutableList.copyOf(authorContributions);
    this.competingInterests = ImmutableList.copyOf(competingInterests);
    this.authors = ImmutableList.copyOf(authors);
  }
}