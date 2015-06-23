package org.ambraproject.rhino.service.taxonomy.impl;

import org.ambraproject.ApplicationException;
import org.ambraproject.rhino.service.taxonomy.TaxonomyLookupService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.util.CategoryUtils;
import org.ambraproject.views.CategoryView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * This is a separate bean from {@link TaxonomyServiceImpl} because it depends on the legacyTaxonomyService, which calls
 * out to Solr.
 */
public class TaxonomyLookupServiceImpl implements TaxonomyLookupService {

  // TODO: get this to work with the @Autowired annotation instead of passing the TaxonomyService
  // into the constructor.  The issue right now is that TaxonomyService is in the ambra-base
  // codebase, and is spring-configured through an XML file, while our other spring beans
  // (that are @Autowired) are configured in the annotation-based way through
  // {@link RhinoConfiguration}.

  private final org.ambraproject.service.taxonomy.TaxonomyService legacyTaxonomyService;

  public TaxonomyLookupServiceImpl(org.ambraproject.service.taxonomy.TaxonomyService legacyTaxonomyService) {
    this.legacyTaxonomyService = legacyTaxonomyService;
  }

  /**
   * Simple private class used for serialization of results from the read method.
   */
  private static class Result implements Comparable<Result> {
    private final String subject;
    private final int childCount;
    private final long articleCount;

    public Result(String subject, int childCount, long articleCount) {
      this.subject = subject;
      this.childCount = childCount;
      this.articleCount = articleCount;
    }

    public String getSubject() {
      return subject;
    }

    public int getChildCount() {
      return childCount;
    }

    public long getArticleCount() {
      return articleCount;
    }

    public int compareTo(Result that) {
      return subject.compareTo(that.subject);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Transceiver read(final String journal, final String parentArg) throws IOException {
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null; // Unsupported for now
      }

      @Override
      protected Object getData() throws IOException {
        String parent = parentArg;
        CategoryView categoryView;
        try {
          categoryView = legacyTaxonomyService.parseCategories(journal);
        } catch (ApplicationException e) {
          throw new RuntimeException(e);
        }
        if (parent == null) {
          parent = "";
        } else {
          String[] levels = parent.split("/");
          for (String level : levels) {
            categoryView = categoryView.getChild(level);
          }
          if (parent.charAt(0) != '/') {
            parent = '/' + parent;
          }
        }

        Map<String, Long> articleCounts;
        try {
          articleCounts = legacyTaxonomyService.getCounts(categoryView, journal);
        } catch (ApplicationException e) {
          throw new IOException(e);
        }

        Map<String, SortedSet<String>> tree = CategoryUtils.getShortTree(categoryView);
        List<Result> results = new ArrayList<>(tree.size());
        for (Map.Entry<String, SortedSet<String>> entry : tree.entrySet()) {
          long articleCount = articleCounts.get(entry.getKey());
          results.add(new Result(parent + '/' + entry.getKey(), entry.getValue().size(), articleCount));
        }
        Collections.sort(results);
        return results;
      }
    };
  }

}
