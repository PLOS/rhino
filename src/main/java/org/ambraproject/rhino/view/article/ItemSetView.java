/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.view.article;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
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
  private final ImmutableCollection<FileView> ancillary;

  private ItemSetView(ArticleIngestion ingestion, Collection<ArticleFile> files) {
    Map<ArticleItem, List<ArticleFile>> itemFileGroups = files.stream()
        .filter(file -> file.getItem() != null)
        .collect(Collectors.groupingBy(ArticleFile::getItem));
    validateItemParentage(ingestion, itemFileGroups.keySet());
    this.items = itemFileGroups.entrySet().stream()
        .collect(ImmutableMap.toImmutableMap(
            entry -> entry.getKey().getDoi(),
            entry -> new ItemView(entry.getKey(), entry.getValue())));

    this.ancillary = files.stream()
        .filter(file -> file.getItem() == null)
        .map(FileView::new)
        .collect(ImmutableList.toImmutableList());
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
    private final String doi;
    private final String itemType;
    private final ImmutableMap<String, FileView> files;

    private ItemView(ArticleItem item, Collection<ArticleFile> files) {
      this.doi = Objects.requireNonNull(item.getDoi());
      this.itemType = Objects.requireNonNull(item.getItemType());
      this.files = buildFileViewMap(item, files);
    }

    private static ImmutableMap<String, FileView> buildFileViewMap(ArticleItem item, Collection<ArticleFile> files) {
      Map<String, FileView> builder = Maps.newLinkedHashMapWithExpectedSize(files.size());
      for (ArticleFile file : files) {
        String type = file.getFileType();
        FileView view = new FileView(file);
        FileView previous = builder.put(type, view);
        if (previous != null) {
          String message = String.format("Item has more than one file with file type \"%s\": %s",
              type, item.getDoi());
          throw new RuntimeException(message);
        }
      }
      return ImmutableMap.copyOf(builder);
    }
  }

  private static class FileView {

    private final long size;

    private FileView(ArticleFile file) {
      this.size = file.getFileSize();
      // Do not expose file.ingestedFileName
    }
  }

}
