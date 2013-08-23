package org.ambraproject.rhino.view.asset;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Collection;
import java.util.List;

public class AssetsAsFigureView implements JsonOutputView {

  private final ImmutableList<ArticleAsset> assets;
  private final Figure figure;

  public AssetsAsFigureView(Collection<ArticleAsset> assets) {
    this.assets = ImmutableList.copyOf(assets);

    List<Figure> figures = Figure.listFigures(this.assets);
    if (figures.size() != 1) {
      String message = "ArticleAsset objects passed to AssetsAsFigureView must all have one DOI in common";
      throw new IllegalArgumentException(message);
    }
    this.figure = figures.get(0);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    AssetIdentity assetId = figure.getId();
    ArticleAsset original = figure.getOriginal();

    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", assetId.getIdentifier());
    serialized.addProperty("contextElement", original.getContextElement());
    serialized.addProperty("title", original.getTitle());
    serialized.addProperty("description", original.getDescription());

    serialized.add("assetfiles", context.serialize(new AssetFileCollectionView(assets)));

    return serialized;
  }

}
