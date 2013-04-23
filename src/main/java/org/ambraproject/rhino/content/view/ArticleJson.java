package org.ambraproject.rhino.content.view;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import org.ambraproject.models.Article;
import org.ambraproject.models.Syndication;

/*
 * Constants.
 */
interface ArticleJson {

  /**
   * The integer constants that represent publication states, as given in {@link Article}, mapped onto names chosen for
   * this API.
   */
  public static final ImmutableBiMap<Integer, String> PUBLICATION_STATE_CONSTANTS = ImmutableBiMap.<Integer, String>builder()
      .put(Article.STATE_ACTIVE, "published")
      .put(Article.STATE_UNPUBLISHED, "ingested")
      .put(Article.STATE_DISABLED, "disabled")
      .build();

  /**
   * API names for {@link Article} publication states, mapped onto their integer representations.
   */
  public static final ImmutableBiMap<String, Integer> PUBLICATION_STATE_NAMES = PUBLICATION_STATE_CONSTANTS.inverse();

  /**
   * Names of JSON members that are meaningful for input. (Other member names appearing in output are auto-generated
   * when Gson serialized an {@link Article} object.
   */
  public static interface MemberNames {
    public static final String DOI = "doi";
    public static final String STATE = "state";
    public static final String SYNDICATIONS = "syndications";
    public static final String SYNDICATION_TARGET = "target";
    public static final String SYNDICATION_STATUS = "status";
  }

  /**
   * Valid values for {@link Syndication#setStatus}.
   */
  public static final ImmutableSet<String> SYNDICATION_STATUSES = ImmutableSet.of(Syndication.STATUS_PENDING,
      Syndication.STATUS_IN_PROGRESS, Syndication.STATUS_SUCCESS, Syndication.STATUS_FAILURE);

}
