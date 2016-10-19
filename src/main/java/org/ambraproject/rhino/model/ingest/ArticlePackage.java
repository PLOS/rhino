package org.ambraproject.rhino.model.ingest;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.Doi;

import java.util.List;
import java.util.Objects;

public class ArticlePackage {

  private final ArticleItemInput articleItem;
  private final ImmutableList<ArticleItemInput> allItems;
  private final ImmutableList<ArticleFileInput> ancillaryFiles;

  ArticlePackage(ArticleItemInput articleItem, List<ArticleItemInput> assetItems, List<ArticleFileInput> ancillaryFiles) {
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

  public ImmutableList<ArticleFileInput> getAncillaryFiles() {
    return ancillaryFiles;
  }

}
