package org.ambraproject.rhino.view.asset;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AssetCollectionView implements JsonOutputView {

  /**
   * AnnotationTypes that are considered corrections.
   */
  private static final ImmutableSet<AnnotationType> CORRECTION_TYPES = Sets.immutableEnumSet(
      AnnotationType.FORMAL_CORRECTION, AnnotationType.MINOR_CORRECTION, AnnotationType.RETRACTION);

  private final ImmutableListMultimap<String, ArticleAsset> assets;

  private final ImmutableMap<String, Annotation> corrections;

  public AssetCollectionView(Article article, List<Annotation> annotations) {
    Iterable<ArticleAsset> assets = article.getAssets();
    ImmutableListMultimap.Builder<String, ArticleAsset> buffer = ImmutableListMultimap.builder();
    for (ArticleAsset asset : assets) {
      buffer.put(asset.getDoi(), asset);
    }
    this.assets = buffer.build();

    // TODO: comments.  Right now only deal with correction annotations.
    ImmutableMap.Builder<String, Annotation> builder = ImmutableMap.builder();
    int correctionNum = 1;
    for (Annotation annotation : annotations) {
      if (CORRECTION_TYPES.contains(annotation.getType())) {
        builder.put(getCorrectionKey(article, annotation, correctionNum++), annotation);
      }
    }
    this.corrections = builder.build();
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
    JsonObject correctionsMap = new JsonObject();
    for (Map.Entry<String, Annotation> entry : corrections.entrySet()) {
      correctionsMap.add(entry.getKey(), context.serialize(entry.getValue()));
    }
    serialized.add("corrections", correctionsMap);
    return serialized;
  }

  /**
   * Generates the pseudo-DOI used to identify a correction associated with an article.
   *
   * @param article article the correction is associated with
   * @param annotation correction annotation
   * @param sequenceNum number of the correction
   * @return the pseudo-DOI that will be used as a key when serializing
   */
  private String getCorrectionKey(Article article, Annotation annotation, int sequenceNum) {
    return String.format("%s.%s.%04d", article.getDoi(), annotation.getType().toString(), sequenceNum);
  }
}
