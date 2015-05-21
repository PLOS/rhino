package org.ambraproject.rhino.service.taxonomy;

import org.ambraproject.models.Article;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service for assigning taxonomic terms to an article.
 */
public interface TaxonomyClassificationService {

  /**
   * Classify an article from its xml.
   *
   * @param articleXml the article xml
   * @param article article object from Ambra
   * @return a map of categories to which the article belongs. Each entry should use <code>/</code>s to delimit subject
   * hierarchy.  Categories are returned in descending order of the strength of the match paired with the strength
   * value
   * @throws TaxonomyClassificationServiceNotConfiguredException if a remote service is required but not configured
   */
  public Map<String, Integer> classifyArticle(Document articleXml, Article article) throws IOException;

  /**
   * Queries the MAI server for taxonomic terms for a given article, and returns a list of the raw results.
   *
   * @param articleXml DOM of the article to categorize
   * @param article article object from Ambra
   * @return List of results from the server.  This will consist of raw XML fragments, and include things like counts
   * that we don't currently store in mysql.
   * @throws IOException
   */
  public List<String> getRawTerms(Document articleXml, Article article) throws IOException;

  /**
   * Indicates that a remote service is required to classify articles but is not configured on this system.
   */
  public static class TaxonomyClassificationServiceNotConfiguredException extends RuntimeException {
    public TaxonomyClassificationServiceNotConfiguredException() {
      super();
    }
  }

}
