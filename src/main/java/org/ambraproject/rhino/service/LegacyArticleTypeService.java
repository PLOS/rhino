package org.ambraproject.rhino.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.util.NlmArticleTypes;
import org.ambraproject.rhino.util.response.Transceiver;
import org.apache.commons.configuration.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Retrieve article types using the imported org.ambraproject.views.article.ArticleType code.
 * <p/>
 * This implementation receives article type metadata from {@code ambra.xml} and stores it in global, static
 * collections. It ought to be rearchitected, but for now we want to just shove it under the {@link ArticleTypeService}
 * interface.
 */
public class LegacyArticleTypeService implements ArticleTypeService {
  private final ImmutableMap<URI, ArticleType> knownArticleTypes;
  private final ImmutableList<ArticleType> articleTypeOrder;

  public LegacyArticleTypeService(Configuration ambraConfiguration) {
    Map<URI, ArticleType> knownArticleTypes = new LinkedHashMap<>();
    List<ArticleType> articleTypeOrder = new ArrayList<>();

    int count = 0;
    String basePath = "ambra.articleTypeList.articleType";
    String uriStr;

    /*
     * Iterate through the defined article types. This is ugly since the index needs
     * to be given in xpath format to access the element, so we calculate a base string
     * like: ambra.articleTypeList.articleType(x) and check if it's non-null for typeUri
     */
    do {
      String baseString = basePath + "(" + count + ").";
      uriStr = ambraConfiguration.getString(baseString + "typeUri");
      String headingStr = ambraConfiguration.getString(baseString + "typeHeading");
      String pluralHeadingStr = ambraConfiguration.getString(baseString + "typeHeadingPlural");
      String codeStr = ambraConfiguration.getString(baseString + "typeCode");
      if ((uriStr != null) && (headingStr != null)) {
        URI uri = URI.create(uriStr);
        final ArticleType at;
        if (!knownArticleTypes.containsKey(uri)) {
          at = new ArticleType(uri, headingStr, pluralHeadingStr, codeStr);
          knownArticleTypes.put(uri, at);
          articleTypeOrder.add(at);
        }
      }
      count++;
    } while (uriStr != null);

    this.knownArticleTypes = ImmutableMap.copyOf(knownArticleTypes);
    this.articleTypeOrder = ImmutableList.copyOf(articleTypeOrder);
  }

  @Override
  public String getNlmArticleType(Article article) {
    String matchedType = null;
    for (String typeString : article.getTypes()) {
      int slashIndex = typeString.lastIndexOf('/');
      if (slashIndex < 0) throw new ArticleTypeException("Article type URI has no slash");
      String nlmTypeCandidate = typeString.substring(slashIndex + 1);
      if (NlmArticleTypes.TYPES.contains(nlmTypeCandidate)) {
        if (matchedType == null) {
          matchedType = nlmTypeCandidate;
        } else {
          String message = String.format(
              "Multiple article type URIs belonging to the same article (DOI=%s) were NLM article-type attributes: %s, %s",
              article.getDoi(), matchedType, nlmTypeCandidate);
          throw new ArticleTypeException(message);
        }
      }
    }
    return matchedType;
  }

  private ArticleType getMetadataForUri(URI uri) {
    return knownArticleTypes.get(uri);
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Returns an article type that is defined in the system's {@code ambra.xml} file, which is assumed to be unique for
   * each article.
   */
  @Override
  public ArticleType getArticleType(Article article) {
    Set<String> typeUriStrings = article.getTypes();

    /*
     * We expect at most one of these strings to be a "known" article type per the legacy implementation.
     * Return the metadata for that one.
     */
    ArticleType matchedType = null;
    for (String typeUriString : typeUriStrings) {
      URI typeUri;
      try {
        typeUri = new URI(typeUriString);
      } catch (URISyntaxException e) {
        String message = "An article type URI had invalid syntax (DOI=" + article.getDoi() + ")";
        throw new ArticleTypeException(message, e);
      }

      ArticleType typeForUri = getMetadataForUri(typeUri);
      if (typeForUri != null) { // it is known
        if (matchedType == null) {
          matchedType = typeForUri; // first hit
        } else {
          String message = String.format("Multiple article type URIs belonging to the same article (DOI=%s) were 'known': <%s>, <%s>",
              article.getDoi(), matchedType.getUri(), typeUri);
          throw new ArticleTypeException(message);
        }
      }
    }
    return matchedType;
  }

  /*
   * return a list of all defined article types, ordered for display
   * // TODO: remove dependency on ambra-base jar when ArticleType objects are persisted in the db
   */
  @Override
  public Transceiver listArticleTypes() throws IOException {
    return new Transceiver() {
      @Override
      protected Collection<? extends ArticleType> getData() {
        return articleTypeOrder;
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  /**
   * Indicates that a precondition about article types in the data layer was violated.
   */
  private static class ArticleTypeException extends RuntimeException {
    private ArticleTypeException(String message) {
      super(message);
    }

    private ArticleTypeException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
