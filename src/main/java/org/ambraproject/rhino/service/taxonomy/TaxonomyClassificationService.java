package org.ambraproject.rhino.service.taxonomy;

import org.ambraproject.rhino.model.ArticleTable;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.List;

/**
 * Service for assigning taxonomic terms to an article.
 */
public interface TaxonomyClassificationService {

  /**
   * Classify an article from its xml.
   *
   * @param article article object from Ambra
   * @param articleXml the article xml
   * @return a map of categories to which the article belongs. Each entry should use <code>/</code>s to delimit subject
   * hierarchy.  Categories are returned in descending order of the strength of the match paired with the strength
   * value
   * @throws TaxonomyRemoteServiceNotConfiguredException if a remote service is required but not configured
   */
  public List<WeightedTerm> classifyArticle(ArticleTable article, Document articleXml);

  /**
   * Queries the MAI server for taxonomic terms for a given article, and returns a list of the raw results.
   * If {@param isTextRequired} is true, it will also return the text that is sent to taxonomy server.
   *
   * @param articleXml DOM of the article to categorize
   * @param article article object from Ambra
   * @param isTextRequired if true, returns the text sent to the taxonomy server for classification
   * @return List of results from the server.  This will consist of raw XML fragments, and include things like counts
   * that we don't currently store in mysql. The first element of the list will be the text sent to the
   * server if  {@param isTextRequired} is true.
   * @throws IOException
   */
  public List<String> getRawTerms(Document articleXml, ArticleTable article,
                                  boolean isTextRequired) throws IOException;

  /**
   * Populates article category information by making a call to the taxonomy server. Will not throw
   * an exception if we cannot communicate or get results from the taxonomy server. Will not
   * request categories for amendments.
   *
   * @param article the Article model instance
   * @param xml     Document representing the article XML
   */
  public void populateCategories(ArticleTable article, Document xml);

}
