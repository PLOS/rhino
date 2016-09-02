package org.ambraproject.rhino.model.ingest;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.Doi;
import org.plos.crepo.model.input.RepoObjectInput;

import java.util.List;
import java.util.Objects;

public class ArticlePackage {

  private final ArticleItemInput articleItem;
  private final ImmutableList<ArticleItemInput> allItems;
  private final ImmutableList<RepoObjectInput> ancillaryFiles;

  ArticlePackage(ArticleItemInput articleItem, List<ArticleItemInput> assetItems, List<RepoObjectInput> ancillaryFiles) {
    this.articleItem = Objects.requireNonNull(articleItem);
    this.allItems = ImmutableList.<ArticleItemInput>builder()
        .add(articleItem).addAll(assetItems).build();
    this.ancillaryFiles = ImmutableList.copyOf(ancillaryFiles);
  }

  public Doi getDoi() {
    return articleItem.getDoi();
  }

  public ImmutableList<ArticleItemInput> getAllItems() {
    return allItems;
  }

  public ImmutableList<RepoObjectInput> getAncillaryFiles() {
    return ancillaryFiles;
  }

}
