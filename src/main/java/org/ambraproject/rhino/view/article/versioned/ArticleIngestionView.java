package org.ambraproject.rhino.view.article.versioned;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.article.AssetMetadata;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.impl.VersionedIngestionService;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.journal.JournalOutputView;
import org.springframework.beans.factory.annotation.Autowired;

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
    private VersionedIngestionService versionedIngestionService;
    @Autowired
    private JournalOutputView.Factory journalOutputViewFactory;

    public ArticleIngestionView getView(ArticleIngestionIdentifier ingestionId) {
      ArticleIngestion ingestion = articleCrudService.readIngestion(ingestionId);
      ArticleMetadata metadata = versionedIngestionService.getArticleMetadata(ingestionId);
      JournalOutputView journal = JournalOutputView.getShallowView(ingestion.getJournal());

      return new ArticleIngestionView(ingestion, metadata, journal);
    }

  }

  private final ArticleIngestion ingestion;
  private final ArticleMetadata metadata;
  private final JournalOutputView journal;

  private ArticleIngestionView(ArticleIngestion ingestion,
                               ArticleMetadata metadata, JournalOutputView journal) {
    Preconditions.checkArgument(ingestion.getArticle().getDoi().equals(metadata.getDoi()));
    this.ingestion = ingestion;
    this.metadata = metadata;
    this.journal = Objects.requireNonNull(journal);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", ingestion.getArticle().getDoi());
    serialized.addProperty("ingestionNumber", ingestion.getIngestionNumber());
    serialized.add("journal", context.serialize(journal));

    ArticleItem strikingImage = ingestion.getStrikingImage();
    if (strikingImage != null) {
      serialized.add("strikingImage", context.serialize(ItemSetView.getItemView(strikingImage)));
    }

    JsonAdapterUtil.copyWithoutOverwriting(context.serialize(metadata).getAsJsonObject(), serialized);

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
