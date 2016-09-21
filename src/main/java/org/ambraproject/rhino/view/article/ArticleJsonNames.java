package org.ambraproject.rhino.view.article;

/**
 * Names of JSON members that are meaningful for input. (Other member names appearing in output are auto-generated when
 * Gson serialized an Article object.
 */
public class ArticleJsonNames {
  private ArticleJsonNames() {
    throw new AssertionError("Not instantiable");
  }

  public static final String DOI = "doi";
  public static final String SYNDICATIONS = "syndications";
  public static final String SYNDICATION_TARGET = "targetQueue";
  public static final String SYNDICATION_STATUS = "status";
  public static final String ARTICLE_TYPE = "articleType";

}
