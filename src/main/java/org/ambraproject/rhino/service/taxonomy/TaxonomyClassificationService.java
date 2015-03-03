package org.ambraproject.rhino.service.taxonomy;

import org.w3c.dom.Document;

import java.io.IOException;
import java.util.Map;

/**
 * Service for assigning taxonomic terms to an article.
 */
public interface TaxonomyClassificationService {

  /**
   * Classify an article from its xml.
   *
   * @param articleXml the article xml
   * @return a map of categories to which the article belongs. Each entry should use <code>/</code>s to delimit subject
   * hierarchy.  Categories are returned in descending order of the strength of the match paired with the strength
   * value
   * @throws TaxonomyClassificationServiceNotConfiguredException if a remote service is required but not configured
   */
  public Map<String, Integer> classifyArticle(Document articleXml) throws IOException;

  /**
   * Indicates that a remote service is required to classify articles but is not configured on this system.
   */
  public static class TaxonomyClassificationServiceNotConfiguredException extends RuntimeException {
    public TaxonomyClassificationServiceNotConfiguredException() {
      super();
    }
  }

}
