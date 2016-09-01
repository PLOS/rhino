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
      Collection<ArticleFile> files = (List<ArticleFile>) hibernateTemplate.find("FROM ArticleFile WHERE ingestion = ?", ingestion);
      return new ItemSetView(ingestion, files);
    }
  }

  public static ItemView getItemView(ArticleItem item) {
    return new ItemView(item, item.getFiles());
  }

  private final ImmutableMap<String, ItemView> items;
  private final ImmutableCollection<FileView> archival;

  private ItemSetView(ArticleIngestion ingestion, Collection<ArticleFile> files) {
    Map<ArticleItem, List<ArticleFile>> itemFileGroups = files.stream()
        .filter(file -> file.getItem() != null)
        .collect(Collectors.groupingBy(ArticleFile::getItem));
    validateItemParentage(ingestion, itemFileGroups.keySet());
    this.items = ImmutableMap.copyOf(itemFileGroups.entrySet().stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().getDoi(),
            entry -> new ItemView(entry.getKey(), entry.getValue()))));

    this.archival = ImmutableList.copyOf(files.stream()
        .filter(file -> file.getItem() == null)
        .map(FileView::new)
        .collect(Collectors.toList()));
  }

  /**
   * Validate that all items (belonging to a set of files) have the expected parent ingestion.
   */
  private static void validateItemParentage(ArticleIngestion ingestion, Collection<ArticleItem> items) {
    for (ArticleItem item : items) {
      ArticleIngestion itemIngestion = item.getIngestion();
      if (!itemIngestion.equals(ingestion)) {
        String message = String.format("Data integrity violation: item %s has ingestion (%s, %d); expected (%s, %d)", item.getDoi(),
            itemIngestion.getArticle().getDoi(), itemIngestion.getIngestionNumber(),
            ingestion.getArticle().getDoi(), ingestion.getIngestionNumber());
        throw new RuntimeException(message);
      }
    }
  }

  public static class ItemView {
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
    private final long size;

    private FileView(ArticleFile file) {
      this.bucketName = Objects.requireNonNull(file.getBucketName());
      this.crepoKey = Objects.requireNonNull(file.getCrepoKey());
      this.crepoUuid = Objects.requireNonNull(file.getCrepoUuid());
      this.size = file.getFileSize();
    }
  }

}
