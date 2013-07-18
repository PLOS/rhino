package org.ambraproject.rhino.view.article;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import org.ambraproject.models.Article;
import org.ambraproject.models.Syndication;

public class ArticleJsonConstants {
  private ArticleJsonConstants() {
    throw new AssertionError("Not instantiable");
  }

  /**
   * The integer constants that represent publication states, as given in {@link Article}, mapped onto names chosen for
   * this API.
   */
  private static final ImmutableBiMap<Integer, String> PUBLICATION_STATE_CONSTANT_MAP = ImmutableBiMap.<Integer, String>builder()
      .put(Article.STATE_ACTIVE, "published")
      .put(Article.STATE_UNPUBLISHED, "ingested")
      .put(Article.STATE_DISABLED, "disabled")
      .build();
  private static final ImmutableBiMap<String, Integer> PUBLICATION_STATE_NAME_MAP = PUBLICATION_STATE_CONSTANT_MAP.inverse();

  /**
   * Valid back-end constants representing publication states.
   */
  public static final ImmutableSet<Integer> PUBLICATION_STATE_CONSTANTS = PUBLICATION_STATE_CONSTANT_MAP.keySet();

  /**
   * Valid front-end names for publication states. Note that these values are all lowercase, but client input is
   * case-insensitive.
   */
  public static final ImmutableSet<String> PUBLICATION_STATE_NAMES = PUBLICATION_STATE_NAME_MAP.keySet();

  /**
   * Get a user-facing name for a publication state for a back-end integer representation. Returns {@code null} if the
   * argument is not contained in {@link #PUBLICATION_STATE_CONSTANTS}.
   *
   * @param constant a publication state code
   * @return a user-facing publication state name, or {@code null} if argument is unmatched
   * @throws NullPointerException if the argument is null
   */
  public static String getPublicationStateName(Integer constant) {
    return PUBLICATION_STATE_CONSTANT_MAP.get(Preconditions.checkNotNull(constant));
  }

  /**
   * Get the back-end integer representation for user input. Returns {@code null} if the argument is not contained in
   * {@link #PUBLICATION_STATE_NAMES}.
   *
   * @param name a case-insensitive name for a publication state
   * @return a publication state code, or {@code null} if argument is unmatched
   * @throws NullPointerException if the argument is null
   */
  public static Integer getPublicationStateConstant(String name) {
    return PUBLICATION_STATE_NAME_MAP.get(name.toLowerCase());
  }


  /**
   * Names of JSON members that are meaningful for input. (Other member names appearing in output are auto-generated
   * when Gson serialized an {@link Article} object.
   */
  static class MemberNames {
    private MemberNames() {
      throw new AssertionError("Not instantiable");
    }

    public static final String DOI = "doi";
    public static final String STATE = "state";
    public static final String SYNDICATIONS = "syndications";
    public static final String SYNDICATION_TARGET = "target";
    public static final String SYNDICATION_STATUS = "status";
    public static final String PINGBACKS = "pingbacks";
    public static final String ARTICLE_TYPE = "articleType";
  }

  /**
   * Valid values for {@link Syndication#setStatus}.
   */
  public static final ImmutableSet<String> SYNDICATION_STATUSES = ImmutableSet.of(Syndication.STATUS_PENDING,
      Syndication.STATUS_IN_PROGRESS, Syndication.STATUS_SUCCESS, Syndication.STATUS_FAILURE);

}
