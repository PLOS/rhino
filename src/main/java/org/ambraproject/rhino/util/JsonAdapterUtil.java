package org.ambraproject.rhino.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.ambraproject.models.AmbraEntity;
import org.ambraproject.models.ArticleRelationship;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleInputView;
import org.ambraproject.rhino.view.article.ArticleOutputView;
import org.ambraproject.rhino.view.article.ArticleStateView;
import org.ambraproject.rhino.view.article.ArticleViewList;
import org.ambraproject.rhino.view.article.DoiList;
import org.ambraproject.rhino.view.asset.groomed.AssetFileView;
import org.ambraproject.rhino.view.asset.groomed.GroomedFigureView;
import org.ambraproject.rhino.view.asset.raw.AssetCollectionView;
import org.ambraproject.rhino.view.asset.raw.AssetFileCollectionView;
import org.ambraproject.rhino.view.asset.raw.AssetsAsFigureView;
import org.ambraproject.rhino.view.journal.IssueOutputView;
import org.ambraproject.rhino.view.journal.JournalListView;
import org.ambraproject.rhino.view.journal.JournalNonAssocView;
import org.ambraproject.rhino.view.journal.JournalOutputView;
import org.ambraproject.rhino.view.journal.VolumeListView;
import org.ambraproject.rhino.view.journal.VolumeOutputView;
import org.ambraproject.rhombat.gson.Iso8601CalendarAdapter;
import org.ambraproject.rhombat.gson.Iso8601DateAdapter;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

public final class JsonAdapterUtil {
  private JsonAdapterUtil() {
    throw new AssertionError("Not instantiable");
  }

  /**
   * Copy all members that aren't already present.
   *
   * @param source      the object to copy from
   * @param destination the object to copy members to
   * @return {@code destination}
   */
  public static JsonObject copyWithoutOverwriting(JsonObject source, JsonObject destination) {
    Preconditions.checkNotNull(destination);
    for (Map.Entry<String, JsonElement> fromEntry : source.entrySet()) {
      String key = fromEntry.getKey();
      if (!destination.has(key)) {
        destination.add(key, fromEntry.getValue());
      }
    }
    return destination;
  }

  /**
   * @return a Gson object suitable for serializing rhino responses
   */
  public static Gson buildGson() {
    GsonBuilder builder = new GsonBuilder();
    builder.setPrettyPrinting();

    ImmutableSet<Class<? extends JsonOutputView>> outputViews = ImmutableSet.of(
        ArticleOutputView.class, ArticleStateView.class, ArticleViewList.class, AssetCollectionView.class,
        AssetFileCollectionView.class, JournalListView.class,
        JournalNonAssocView.class, JournalNonAssocView.ListView.class, VolumeListView.class,
        VolumeOutputView.class, VolumeOutputView.ListView.class, JournalOutputView.class,
        IssueOutputView.class, IssueOutputView.ListView.class, AssetsAsFigureView.class,
        GroomedFigureView.class, AssetFileView.class);
    for (Class<? extends JsonOutputView> viewClass : outputViews) {
      builder.registerTypeAdapter(viewClass, JsonOutputView.SERIALIZER);
    }

    builder.registerTypeAdapter(DoiList.class, DoiList.ADAPTER);
    builder.registerTypeAdapter(ArticleInputView.class, ArticleInputView.DESERIALIZER);
    builder.registerTypeAdapter(Date.class, new Iso8601DateAdapter());
    builder.registerTypeAdapter(Calendar.class, new Iso8601CalendarAdapter());
    builder.registerTypeAdapter(GregorianCalendar.class, new Iso8601CalendarAdapter());

    builder.setExclusionStrategies(
        new ExclusionStrategy() {
          @Override
          public boolean shouldSkipField(FieldAttributes f) {
            final String name = f.getName();
            if ("ID".equals(name) /* internal to the database */) {
              return true;
            }

            // Prevent infinite recursion on ArticleRelationship.parentArticle
            if (ArticleRelationship.class.isAssignableFrom(f.getDeclaringClass())
                && AmbraEntity.class.isAssignableFrom(f.getDeclaredClass())) {
              return true;
            }

            return false;
          }

          @Override
          public boolean shouldSkipClass(Class<?> clazz) {
            return false;
          }
        }
    );
    return builder.create();
  }
}
