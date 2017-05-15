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

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.CustomMetadataExtractor;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.article.ArticleCustomMetadata;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.article.AssetMetadata;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.journal.JournalOutputView;
import org.plos.crepo.model.metadata.RepoObjectMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Deep view of an article ingestion, including data parsed dynamically from the manuscript.
 * <p>
 * In case of a future need for a shallow view that uses only data is that available from the database, see {@link
 * ArticleRevisionView#serializeIngestion}.
 */
public class ArticleIngestionView implements JsonOutputView {

  public static class Factory {

    @Autowired
    private ArticleCrudService articleCrudService;
    @Autowired
    private CustomMetadataExtractor.Factory customMetadataExtractorFactory;

    public ArticleIngestionView getView(ArticleIngestion ingestion) {
      JournalOutputView journal = JournalOutputView.getView(ingestion.getJournal());

      RepoObjectMetadata objectMetadata = articleCrudService.getManuscriptMetadata(ingestion);
      Document document = articleCrudService.getManuscriptXml(objectMetadata);
      ArticleMetadata metadata;
      ArticleCustomMetadata customMetadata;
      try {
        metadata = new ArticleXml(document).build();
        customMetadata = customMetadataExtractorFactory.parse(document).build();
      } catch (XmlContentException e) {
        throw new RuntimeException(e);
      }

      String bucketName = objectMetadata.getVersion().getId().getBucketName();
      return new ArticleIngestionView(ingestion, metadata, customMetadata, journal, bucketName);
    }

  }

  private final ArticleIngestion ingestion;
  private final ArticleMetadata metadata;
  private final ArticleCustomMetadata customMetadata;
  private final JournalOutputView journal;
  private final String bucketName;

  private ArticleIngestionView(ArticleIngestion ingestion,
                               ArticleMetadata metadata,
                               ArticleCustomMetadata customMetadata,
                               JournalOutputView journal,
                               String bucketName) {
    Preconditions.checkArgument(ingestion.getArticle().getDoi().equals(metadata.getDoi()));
    this.ingestion = ingestion;
    this.metadata = metadata;

    this.customMetadata = Objects.requireNonNull(customMetadata);
    this.journal = Objects.requireNonNull(journal);
    this.bucketName = bucketName;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", ingestion.getArticle().getDoi());
    serialized.addProperty("ingestionNumber", ingestion.getIngestionNumber());
    serialized.add("journal", context.serialize(journal));
    serialized.addProperty("bucketName", bucketName);

    ArticleItem strikingImage = ingestion.getStrikingImage();
    if (strikingImage != null) {
      serialized.add("strikingImage", context.serialize(ItemSetView.getItemView(strikingImage)));
    }

    JsonAdapterUtil.copyWithoutOverwriting(context.serialize(metadata).getAsJsonObject(), serialized);
    JsonAdapterUtil.copyWithoutOverwriting(context.serialize(customMetadata).getAsJsonObject(), serialized);

    serialized.remove("assets");
    List<AssetMetadataView> assetViews = metadata.getAssets().stream().map(AssetMetadataView::new).collect(Collectors.toList());
    serialized.add("assetsLinkedFromManuscript", context.serialize(assetViews));

    return serialized;
  }

  private static class AssetMetadataView {
    private final String doi;
    private final String title;
    private final String description;

    private AssetMetadataView(AssetMetadata assetMetadata) {
      this.doi = assetMetadata.getDoi();
      this.title = assetMetadata.getTitle();
      this.description = assetMetadata.getDescription();
      // Do not include assetMetadata.contextElement
    }
  }

}
