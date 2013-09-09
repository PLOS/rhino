package org.ambraproject.rhino.view.asset.groomed;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;

import java.util.Collection;
import java.util.List;

public class GroomedAssetCollectionView {

  // For Gson's default serialization.
  private final AssetFileView articleXml;
  private final AssetFileView articlePdf;
  private final ImmutableList<GroomedFigureView> figures;
  private final ImmutableList<AssetFileView> miscellaneous;

  private GroomedAssetCollectionView(AssetFileView articleXml,
                                     AssetFileView articlePdf,
                                     List<GroomedFigureView> figures,
                                     List<AssetFileView> miscellaneous) {
    this.articleXml = Preconditions.checkNotNull(articleXml);
    this.articlePdf = articlePdf; // nullable
    this.figures = ImmutableList.copyOf(figures);
    this.miscellaneous = ImmutableList.copyOf(miscellaneous);
  }

  public static GroomedAssetCollectionView create(Article article) {
    final List<ArticleAsset> assets = article.getAssets();

    ArticleAsset articleXml = null;
    ArticleAsset articlePdf = null;
    ListMultimap<String, ArticleAsset> figures = LinkedListMultimap.create();
    List<ArticleAsset> miscellaneous = Lists.newArrayList();

    for (ArticleAsset asset : assets) {
      if (article.getDoi().equals(asset.getDoi())) {
        if ("XML".equalsIgnoreCase(asset.getExtension())) {
          if (articleXml != null) {
            throw new IllegalArgumentException("Multiple articleXml assets matched");
          }
          articleXml = asset;
        } else if ("PDF".equalsIgnoreCase(asset.getExtension())) {
          if (articlePdf != null) {
            throw new IllegalArgumentException("Multiple articlePdf assets matched");
          }
          articlePdf = asset;
        } else {
          miscellaneous.add(asset);
        }
      } else if (FigureType.getAllExtensions().contains(asset.getExtension())) {
        figures.put(asset.getDoi(), asset);
      } else {
        miscellaneous.add(asset);
      }
    }

    // By legacy convention, these asset files have figure fields that are redundant to the article itself.
    // So, suppress those fields the same way as for a figure thumbnail.
    AssetFileView articleXmlView = AssetFileView.create(articleXml);
    AssetFileView articlePdfView = AssetFileView.create(articlePdf);

    List<GroomedFigureView> figureViews = Lists.newArrayListWithCapacity(figures.keySet().size());
    for (Collection<ArticleAsset> figureAssetCollection : figures.asMap().values()) {
      figureViews.add(GroomedFigureView.create(figureAssetCollection));
    }

    List<AssetFileView> miscellaneousViews = Lists.newArrayListWithCapacity(miscellaneous.size());
    for (ArticleAsset asset : miscellaneous) {
      // Leave in figure-specific fields; they don't show up anywhere else.
      miscellaneousViews.add(AssetFileView.createWithFullMetadata(asset));
    }

    return new GroomedAssetCollectionView(articleXmlView, articlePdfView, figureViews, miscellaneousViews);
  }

}