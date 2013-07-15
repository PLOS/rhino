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

  private final List<String> commentDois;

  public AssetCollectionView(Article article, List<Annotation> annotations) {
    Iterable<ArticleAsset> assets = article.getAssets();
    ImmutableListMultimap.Builder<String, ArticleAsset> buffer = ImmutableListMultimap.builder();
    for (ArticleAsset asset : assets) {
      buffer.put(asset.getDoi(), asset);
    }
    this.assets = buffer.build();

    correctionDois = getAnnotationDoisByType(annotations, AnnotationType.FORMAL_CORRECTION,
        AnnotationType.MINOR_CORRECTION, AnnotationType.RETRACTION);
    commentDois = getAnnotationDoisByType(annotations, AnnotationType.COMMENT);
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

    serialized.add("corrections", serializeAsList(context, correctionDois));
    serialized.add("comments", serializeAsList(context, commentDois));
    return serialized;
  }

  /**
   * Returns a list of all the DOIs for annotations that have at least one of the given types.
   *
   * @param annotations input Annotation objects
   * @param types any of these AnnotationTypes will be selected
   * @return list of DOIs of Annotations that match
   */
  private List<String> getAnnotationDoisByType(List<Annotation> annotations, AnnotationType... types) {
    List doisMutable = new ArrayList<String>();
    for (Annotation annotation : annotations) {

      // Not using a Set since most of the time, this method will only get called with a single
      // annotation type.  For corrections, they'll be three.
      for (AnnotationType type : types) {
        if (type.equals(annotation.getType())) {
          doisMutable.add(annotation.getAnnotationUri());
          break;
        }
      }
    }
    return Collections.unmodifiableList(doisMutable);
  }

  /**
   * Serializes the given List of Strings as a JSON array.
   *
   * @param context JsonSerializationContext
   * @param strings Strings to serialize
   * @return JsonArray containing the given Strings
   */
  private JsonArray serializeAsList(JsonSerializationContext context, List<String> strings) {
    JsonArray results = new JsonArray();
    for (String s : strings) {
      results.add(context.serialize(s));
    }
    return results;
  }
}
