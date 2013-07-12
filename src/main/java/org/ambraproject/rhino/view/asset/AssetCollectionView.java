package org.ambraproject.rhino.view.asset;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Annotation;
import org.ambraproject.models.AnnotationType;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AssetCollectionView implements JsonOutputView {

  /**
   * AnnotationTypes that are considered corrections.
   */
  private static final ImmutableSet<AnnotationType> CORRECTION_TYPES = Sets.immutableEnumSet(
      AnnotationType.FORMAL_CORRECTION, AnnotationType.MINOR_CORRECTION, AnnotationType.RETRACTION);

  private final ImmutableListMultimap<String, ArticleAsset> assets;

  private final List<String> correctionDois;

  public AssetCollectionView(Article article, List<Annotation> annotations) {
    Iterable<ArticleAsset> assets = article.getAssets();
    ImmutableListMultimap.Builder<String, ArticleAsset> buffer = ImmutableListMultimap.builder();
    for (ArticleAsset asset : assets) {
      buffer.put(asset.getDoi(), asset);
    }
    this.assets = buffer.build();

    // TODO: comments.  Right now only deal with correction annotations.
    List correctionDoisMutable = new ArrayList<String>();
    for (Annotation annotation : annotations) {
      if (CORRECTION_TYPES.contains(annotation.getType())) {
        correctionDoisMutable.add(annotation.getAnnotationUri());
      }
    }
    correctionDois = Collections.unmodifiableList(correctionDoisMutable);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    for (Map.Entry<String, Collection<ArticleAsset>> entry : assets.asMap().entrySet()) {
      String assetId = DoiBasedIdentity.asIdentifier(entry.getKey());
      List<ArticleAsset> assetFiles = (List<ArticleAsset>) entry.getValue(); // cast is safe because it's a ListMultimap
      JsonElement byFileId = AssetFileCollectionView.serializeAssetFiles(assetFiles, context);
      serialized.add(assetId, byFileId);
    }

    // TODO: consider moving this into an AnnotationCollectionView if necessary.
    JsonArray corrections = new JsonArray();
    for (String correctionDoi : correctionDois) {
      corrections.add(context.serialize(correctionDoi));
    }
    serialized.add("corrections", corrections);
    return serialized;
  }
}
