package org.ambraproject.rhino.config.json;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
   * When you define a new {@code JsonOutputView}, you must add it to the array below before Gson will use the
   * interface. (Please follow the existing code style for human-friendliness.)
   */
  private static final Class[] JSON_OUTPUT_VIEW_CLASSES = {

      org.ambraproject.rhino.view.article.ArticleOutputView.class,
      org.ambraproject.rhino.view.article.ArticleStateView.class,
      org.ambraproject.rhino.view.article.ArticleViewList.class,

      org.ambraproject.rhino.view.asset.groomed.GroomedAssetFileView.class,
      org.ambraproject.rhino.view.asset.groomed.GroomedFigureView.class,
      org.ambraproject.rhino.view.asset.raw.RawAssetCollectionView.class,
      org.ambraproject.rhino.view.asset.raw.RawAssetFileCollectionView.class,
      org.ambraproject.rhino.view.asset.raw.RawAssetFileView.class,

      org.ambraproject.rhino.view.journal.IssueOutputView.class,
      org.ambraproject.rhino.view.journal.JournalListView.class,
      org.ambraproject.rhino.view.journal.JournalNonAssocView.class,
      org.ambraproject.rhino.view.journal.JournalOutputView.class,
      org.ambraproject.rhino.view.journal.VolumeListView.class,
      org.ambraproject.rhino.view.journal.VolumeOutputView.class,

  };

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


  @SuppressWarnings("unchecked") // use isAssignableFrom to check parameter correctness manually
  public static ImmutableCollection<Class<? extends JsonOutputView>> getOutputViewClasses() {
    for (Class<?> classObj : JSON_OUTPUT_VIEW_CLASSES) {
      if (!JsonOutputView.class.isAssignableFrom(classObj)) {
        String message = String.format("%s does not implement %s",
            classObj.getName(), JsonOutputView.class.getName());
        throw new AssertionError(message);
      }
    }
    return ImmutableList.<Class<? extends JsonOutputView>>copyOf(JSON_OUTPUT_VIEW_CLASSES);
  }

  public static ImmutableMap<Type, Object> getCustomAdapters() {
    return CUSTOM_ADAPTERS;
  }

}
