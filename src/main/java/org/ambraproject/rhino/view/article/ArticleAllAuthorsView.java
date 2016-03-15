package org.ambraproject.rhino.view.article;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class ArticleAllAuthorsView {

  private final ImmutableList<AuthorView> authors;
  private final ImmutableList<String> authorContributions;
  private final ImmutableList<String> competingInterests;
  private final ImmutableList<String> correspondingAuthorList;

  public ArticleAllAuthorsView(List<AuthorView> authors, List<String> authorContributions,
                               List<String> competingInterests, List<String> correspondingAuthorList) {
    this.authorContributions = ImmutableList.copyOf(authorContributions);
    this.competingInterests = ImmutableList.copyOf(competingInterests);
    this.authors = ImmutableList.copyOf(authors);
    this.correspondingAuthorList = ImmutableList.copyOf(correspondingAuthorList);
  }
}
