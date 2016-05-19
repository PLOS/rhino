package org.ambraproject.rhino.view.asset.groomed;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleAsset;

import java.util.Collection;
import java.util.List;

public class GroomedAssetsView {

  // For Gson's default serialization.
  private final GroomedAssetFileView articleXml;
  private final GroomedAssetFileView articlePdf;
  private final ImmutableList<GroomedImageView> figures;
  private final ImmutableList<GroomedImageView> graphics;
  private final ImmutableList<GroomedAssetFileView> miscellaneousAssetFiles;

  private GroomedAssetsView(GroomedAssetFileView articleXml,
                            GroomedAssetFileView articlePdf,
                            List<GroomedImageView> figures,
                            List<GroomedImageView> graphics,
                            List<GroomedAssetFileView> miscellaneousAssetFiles) {
    this.articleXml = Preconditions.checkNotNull(articleXml);
    this.articlePdf = articlePdf; // nullable
    this.figures = ImmutableList.copyOf(figures);
    this.graphics = ImmutableList.copyOf(graphics);
    this.miscellaneousAssetFiles = ImmutableList.copyOf(miscellaneousAssetFiles);
  }

  public static GroomedAssetsView create(Article article) {
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
      } else if (ImageFileType.getAllExtensions().contains(asset.getExtension())) {
        figures.put(asset.getDoi(), asset);
      } else {
        miscellaneous.add(asset);
      }
    }

    // By legacy convention, these asset files have figure fields that are redundant to the article itself.
    // So, suppress those fields the same way as for a figure thumbnail.
    GroomedAssetFileView articleXmlView = GroomedAssetFileView.create(articleXml);
    GroomedAssetFileView articlePdfView = (articlePdf == null) ? null : GroomedAssetFileView.create(articlePdf);

    Collection<Collection<ArticleAsset>> figureAssetGroups = figures.asMap().values();
    List<GroomedImageView> figureViews = Lists.newArrayList();
    List<GroomedImageView> graphicViews = Lists.newArrayList();
    for (Collection<ArticleAsset> figureAssetGroup : figureAssetGroups) {

      GroomedImageView groomedImageView;
      if (figureAssetGroup.size() == 1) {
        // A figure would have an original image and at least one thumbnail. Assume this is a supp info asset.
        groomedImageView = null;
      } else {
        try {
          groomedImageView = GroomedImageView.create(figureAssetGroup);
        } catch (UncategorizedAssetException e) {
          groomedImageView = null;
        }
      }

      if (groomedImageView == null) {
        // figureAssetGroup doesn't match how we expect a figure to be represented
        miscellaneous.addAll(figureAssetGroup);
      } else {
        List<GroomedImageView> viewType;
        switch (groomedImageView.getImageType()) {
          case FIGURE:
            viewType = figureViews;
            break;
          case GRAPHIC:
            viewType = graphicViews;
            break;
          default:
            throw new AssertionError();
        }
        viewType.add(groomedImageView);
      }
    }

    List<GroomedAssetFileView> miscellaneousViews = Lists.newArrayListWithCapacity(miscellaneous.size());
    for (ArticleAsset asset : miscellaneous) {
      // Leave in figure-specific fields; they don't show up anywhere else.
      miscellaneousViews.add(GroomedAssetFileView.createWithFullMetadata(asset));
    }

    return new GroomedAssetsView(articleXmlView, articlePdfView, figureViews, graphicViews, miscellaneousViews);
  }

}