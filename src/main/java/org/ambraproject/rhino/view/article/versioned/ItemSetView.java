package org.ambraproject.rhino.view.article.versioned;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ItemSetView {

  public static class Factory {
    @Autowired
    private HibernateTemplate hibernateTemplate;

    public ItemSetView getView(ArticleIngestion ingestion) {
      Collection<ArticleItem> items = (List<ArticleItem>) hibernateTemplate.find("FROM ArticleItem WHERE ingestion = ?", ingestion);
      Collection<ArticleFile> files = (List<ArticleFile>) hibernateTemplate.find("FROM ArticleFile WHERE ingestion = ?", ingestion);
      return new ItemSetView(items, files);
    }
  }

  private final ImmutableMap<String, ItemView> items;
  private final ImmutableCollection<FileView> archival;

  private ItemSetView(Collection<ArticleItem> items, Collection<ArticleFile> files) {
    Map<ArticleItem, List<ArticleFile>> itemFileGroups = files.stream()
        .filter(file -> file.getItem() != null)
        .collect(Collectors.groupingBy(ArticleFile::getItem));
    if (!items.containsAll(itemFileGroups.keySet())) {
      throw new RuntimeException("Data integrity violation: files and items have inconsistent ingestions");
    }
    this.items = ImmutableMap.copyOf(itemFileGroups.entrySet().stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().getDoi(),
            entry -> new ItemView(entry.getKey(), entry.getValue()))));

    this.archival = ImmutableList.copyOf(files.stream()
        .filter(file -> file.getItem() == null)
        .map(FileView::new)
        .collect(Collectors.toList()));
  }

  private static class ItemView {
    private final String itemType;
    private final ImmutableMap<String, FileView> files;

    private ItemView(ArticleItem item, Collection<ArticleFile> files) {
      this.itemType = Objects.requireNonNull(item.getItemType());
      this.files = ImmutableMap.copyOf(files.stream()
          .collect(Collectors.toMap(ArticleFile::getFileType, FileView::new)));
    }
  }

  private static class FileView {
    private final String bucketName;
    private final String crepoKey;
    private final String crepoUuid;

    private FileView(ArticleFile file) {
      this.bucketName = Objects.requireNonNull(file.getBucketName());
      this.crepoKey = Objects.requireNonNull(file.getCrepoKey());
      this.crepoUuid = Objects.requireNonNull(file.getCrepoUuid());
    }
  }

}
