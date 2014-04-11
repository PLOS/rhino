package org.ambraproject.rhino.config.json;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.view.AnnotationOutputView;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleInputView;
import org.ambraproject.rhino.view.article.DoiList;

import java.lang.reflect.Type;

/**
 * Statically-defined collections of adapter objects to provide to Gson in configuration.
 *
 * @see org.ambraproject.rhino.config.RhinoConfiguration#entityGson()
 */
public class AdapterRegistry {
  private AdapterRegistry() {
    throw new AssertionError("Not instantiable");
  }

  /**
   * A list of all classes that implement that {@link JsonOutputView} interface and should be serialized with its {@code
   * serialize} method.
   * <p/>
   * It would be nice to configure Gson to apply the adapter to any object implementing the interface by default. It is
   * a little unfortunate that listing them here seems to be necessary, since it violates the principle that an
   * interface shouldn't have to know about its implementations. But Gson's API seems to necessitate it, unless we've
   * missed something so far.
   * <p/>
   * When you define a new {@code JsonOutputView}, you must add it here before Gson will use the interface. (Please
   * follow the existing code style for human-friendliness.)
   */
  private static final ImmutableList<Class<? extends JsonOutputView>> JSON_OUTPUT_VIEW_CLASSES = ImmutableList.<Class<? extends JsonOutputView>>builder()
      /*
       * Using a builder and 'add' calls is the best way to check at compile time that each class
       * implements JsonOutputView. Because of type erasure, this is not possible in an array,
       * including the varargs that would be used in a large ImmutableList.of(...) expression.
       */
      .add(org.ambraproject.rhino.view.JsonWrapper.class)

      .add(org.ambraproject.rhino.view.article.ArticleAuthorView.class)
      .add(org.ambraproject.rhino.view.article.ArticleOutputView.class)
      .add(org.ambraproject.rhino.view.article.ArticleStateView.class)
      .add(org.ambraproject.rhino.view.article.ArticleViewList.class)

      .add(org.ambraproject.rhino.view.asset.groomed.GroomedAssetFileView.class)
      .add(org.ambraproject.rhino.view.asset.groomed.GroomedImageView.class)
      .add(org.ambraproject.rhino.view.asset.raw.RawAssetCollectionView.class)
      .add(org.ambraproject.rhino.view.asset.raw.RawAssetFileCollectionView.class)
      .add(org.ambraproject.rhino.view.asset.raw.RawAssetFileView.class)

      .add(org.ambraproject.rhino.view.journal.IssueOutputView.class)
      .add(org.ambraproject.rhino.view.journal.IssueOutputView.ListView.class)
      .add(org.ambraproject.rhino.view.journal.JournalListView.class)
      .add(org.ambraproject.rhino.view.journal.JournalNonAssocView.class)
      .add(org.ambraproject.rhino.view.journal.JournalNonAssocView.ListView.class)
      .add(org.ambraproject.rhino.view.journal.JournalOutputView.class)
      .add(org.ambraproject.rhino.view.journal.VolumeListView.class)
      .add(org.ambraproject.rhino.view.journal.VolumeOutputView.class)
      .add(org.ambraproject.rhino.view.journal.VolumeOutputView.ListView.class)

      .add(org.ambraproject.rhino.view.AnnotationOutputView.class)

      .build();

  /**
   * A map from data types to custom adapters. An adapter defined here will be applied by default when Gson encounters
   * an object of the given type. This is especially useful when those objects may appear as sub-values of other objects
   * that are serialized by reflection; then these adapters are applied automagically.
   * <p/>
   * Unlike {@link #JSON_OUTPUT_VIEW_CLASSES}, this is not so much an apparent kludge but normal Gson configuration.
   */
  private static final ImmutableMap<Type, Object> CUSTOM_ADAPTERS = ImmutableMap.<Type, Object>builder()
      .put(DoiList.class, DoiList.ADAPTER)
      .put(ArticleInputView.class, ArticleInputView.DESERIALIZER)
      .build();


  public static ImmutableCollection<Class<? extends JsonOutputView>> getOutputViewClasses() {
    return JSON_OUTPUT_VIEW_CLASSES;
  }

  public static ImmutableMap<Type, Object> getCustomAdapters() {
    return CUSTOM_ADAPTERS;
  }

}
