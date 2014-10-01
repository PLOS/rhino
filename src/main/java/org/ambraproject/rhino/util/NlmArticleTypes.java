package org.ambraproject.rhino.util;

import com.google.common.collect.ImmutableSet;

/**
 * Constants specified by the NLM DTD for the {@code article-type} field.
 * <p/>
 * Storing these here is a hack. It would be better to properly import them from the DTD associated with a particular
 * article, and to be flexible as to which version of the DTD to take them from. Fortunately, the list is stable across
 * the versions that our ingestion code supports (2.3 and 3.0).
 * <p/>
 * These are programmatically significant because of the unfortunate way that the legacy data model mixes together what
 * it considers to be "article types". That is, both of the values (in XPath) {@code /article/@article-type} and {@code
 * "/article/front/article-meta/article-categories/subj-group[@subj-group-type = 'heading']/subject"} are mingled
 * together as article types (after being converted to URIs with the prefix {@code
 * "http://rdf.plos.org/RDF/articleType/"}). Because the first group has a closed set of legal values, the set is useful
 * for distinguishing them. (There is no guarantee that the second group wouldn't also have a value from the same set,
 * but they generally begin with a capital letter, so it hopefully is a safe enough to assume that an article type is
 * from the first group if and only if it belongs to the specified set.)
 */
public class NlmArticleTypes {
  private NlmArticleTypes() {
    throw new AssertionError("Not instantiable");
  }

  public static final ImmutableSet<String> TYPES = ImmutableSet.copyOf(new String[]{
      "abstract", "addendum", "announcement", "article-commentary", "book-review", "books-received",
      "brief-report", "calendar", "case-report", "collection", "correction", "discussion", "dissertation",
      "editorial", "in-brief", "introduction", "letter", "meeting-report", "news", "obituary", "oration",
      "other", "partial-retraction", "product-review", "rapid-communication", "reply", "reprint",
      "research-article", "retraction", "review-article", "translation",
  });

}
