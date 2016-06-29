package org.ambraproject.rhino.view.article;

import org.ambraproject.rhino.model.Article;

/**
 * Names of JSON members that are meaningful for input. (Other member names appearing in output are auto-generated when
 * Gson serialized an {@link Article} object.
 */
public class ArticleJsonNames {
  private ArticleJsonNames() {
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
