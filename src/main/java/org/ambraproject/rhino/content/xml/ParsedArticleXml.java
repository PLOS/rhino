package org.ambraproject.rhino.content.xml;

import org.ambraproject.models.Article;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ParsedArticleXml {

  private final Article article;
  private final Map<String, AssetText> assets;

  public ParsedArticleXml(Article article) {
    this.article = Objects.requireNonNull(article);
    this.assets = new LinkedHashMap<>();
  }

  public Article getArticle() {
    return article;
  }

  public static final class AssetText {
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ParsedArticleXml)) return false;

    ParsedArticleXml that = (ParsedArticleXml) o;
    if (!article.equals(that.article)) return false;
    if (!assets.equals(that.assets)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = article.hashCode();
    result = 31 * result + assets.hashCode();
    return result;
  }

}
