package org.ambraproject.rhino.config.json;

import com.google.common.collect.ImmutableSet;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import org.ambraproject.models.AmbraEntity;
import org.ambraproject.models.ArticleRelationship;
import org.ambraproject.models.Journal;
import org.ambraproject.models.UserProfile;

/**
 * Exclusions for classes and fields when using Gson's default, reflection-based serialization logic.
 *
 * @see org.ambraproject.rhino.config.RhinoConfiguration#entityGson()
 * @see com.google.gson.GsonBuilder#setExclusionStrategies(com.google.gson.ExclusionStrategy...)
 */
public enum ExclusionSpecialCase implements ExclusionStrategy {

  /**
   * Leave out the primary key on all classes, which is internal to the database and should not be meaningful to any
   * client.
   */
  PRIMARY_KEY {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return "ID".equals(f.getName());
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return false;
    }
  },

  /**
   * When serializing {@code ArticleRelationship} objects, leave out any reference to other persistent entities, because
   * they can cause infinite recursion. Serialize only the primitive {@code ArticleRelationship} fields. Also suppress
   * raw foreign keys.
   */
  ARTICLE_RELATIONSHIP {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return ArticleRelationship.class.isAssignableFrom(f.getDeclaringClass()) &&
          (AmbraEntity.class.isAssignableFrom(f.getDeclaredClass())
              || f.getName().equals("otherArticleID"));
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return false;
    }
  },

  /**
   * Prevent an infinite recursion from {@link Journal} to {@link org.ambraproject.models.ArticleList}.
   */
  JOURNAL_ARTICLE_LIST {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      // Can't look for ArticleList.class because of type erasure. Rely on the name instead.
      return Journal.class.isAssignableFrom(f.getDeclaringClass())
          && f.getName().equals("articleLists");
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return false;
    }
  },

  USER_PROFILE {
    private final ImmutableSet<String> SKIPPED_FIELD_NAMES = ImmutableSet.copyOf(new String[]{
        // Works around bugs with lazily-loaded UserRole objects, specifically when an Annotation object contains a
        // UserProfile object. The roles could be served in the future if needed, but we'd have to fix the bugs.
        "roles",

        // Leave out security-sensitive values. It would not necessarily introduce a vulnerability if we served these
        // ("password" is a salted hash), but, for caution's sake, we await a motivating use case.
        "authId", "password",
    });

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return UserProfile.class.isAssignableFrom(f.getDeclaringClass())
          && SKIPPED_FIELD_NAMES.contains(f.getName());
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return false;
    }
  };

}
