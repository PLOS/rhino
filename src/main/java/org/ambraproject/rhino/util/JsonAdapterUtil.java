package org.ambraproject.rhino.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.ambraproject.models.AmbraEntity;
import org.ambraproject.models.ArticleRelationship;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleInputView;
import org.ambraproject.rhino.view.article.ArticleOutputView;
import org.ambraproject.rhino.view.article.ArticleStateView;
import org.ambraproject.rhino.view.article.ArticleViewList;
import org.ambraproject.rhino.view.article.DoiList;
import org.ambraproject.rhino.view.asset.AssetCollectionView;
import org.ambraproject.rhino.view.asset.AssetFileCollectionView;
import org.ambraproject.rhino.view.journal.IssueOutputView;
import org.ambraproject.rhino.view.journal.JournalListView;
import org.ambraproject.rhino.view.journal.JournalNonAssocView;
import org.ambraproject.rhino.view.journal.JournalOutputView;
import org.ambraproject.rhino.view.journal.VolumeListView;
import org.ambraproject.rhino.view.journal.VolumeOutputView;

import javax.xml.bind.DatatypeConverter;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;

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
   * Serializer/deserializer that converts between Java Date objects and ISO 8601
   * compliant date strings.  The date strings are expressed in the UTC timezone,
   * regardless of the local timezone.
   * <p/>
   * TODO: we will probably need to move this somewhere where it can be shared with
   * callers to rhino.
   * <p/>
   * Adapted from code at http://code.google.com/p/google-gson/issues/detail?id=281
   */
  private static final class Iso8601DateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

    // Implementation note: it would be most straightforward to use a SimpleDateFormat
    // here, but prior to Java 7, it is not possible for this class to correctly
    // parse ISO 8601 dates in the UTC timezone.
    // TODO: after we upgrade to Java 7, consider using something like the following line:
    // dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);

    @Override
    public synchronized JsonElement serialize(Date date, Type type,
        JsonSerializationContext jsonSerializationContext) {
      Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
      calendar.setTime(date);
      return new JsonPrimitive(DatatypeConverter.printDateTime(calendar));
    }

    @Override
    public synchronized Date deserialize(JsonElement jsonElement, Type type,
        JsonDeserializationContext jsonDeserializationContext) {
      Calendar calendar = DatatypeConverter.parseDateTime(jsonElement.getAsString());
      calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
      return calendar.getTime();
    }
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
        IssueOutputView.class, IssueOutputView.ListView.class);
    for (Class<? extends JsonOutputView> viewClass : outputViews) {
      builder.registerTypeAdapter(viewClass, JsonOutputView.SERIALIZER);
    }

    builder.registerTypeAdapter(DoiList.class, DoiList.ADAPTER);
    builder.registerTypeAdapter(ArticleInputView.class, ArticleInputView.DESERIALIZER);
    builder.registerTypeAdapter(Date.class, new Iso8601DateAdapter());

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
