/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service.impl;

import com.google.common.base.Strings;
import org.ambraproject.ApplicationException;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.service.ClassificationService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.service.taxonomy.TaxonomyService;
import org.ambraproject.util.CategoryUtils;
import org.ambraproject.views.CategoryView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * {@inheritDoc}
 */
public class ClassificationServiceImpl extends AmbraService implements ClassificationService {

  // TODO: get this to work with the @Autowired annotation instead of passing the TaxonomyService
  // into the constructor.  The issue right now is that TaxonomyService is in the ambra-base
  // codebase, and is spring-configured through an XML file, while our other spring beans
  // (that are @Autowired) are configured in the annotation-based way through
  // {@link RhinoConfiguration}.

  private TaxonomyService taxonomyService;

  public ClassificationServiceImpl(TaxonomyService taxonomyService) {
    this.taxonomyService = taxonomyService;
  }

  /**
   * Simple private class used for serialization of results from the read method.
   */
  private static class Result implements Comparable<Result> {
    public String subject;
    public int childCount;

    Result(String subject, int childCount) {
      this.subject = subject;
      this.childCount = childCount;
    }

    public int compareTo(Result that) {
      return subject.compareTo(that.subject);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void read(String journal, String parent, ResponseReceiver receiver, MetadataFormat format)
      throws IOException, ApplicationException {
    CategoryView categoryView = taxonomyService.parseCategories(journal);
    if (!Strings.isNullOrEmpty(parent)) {
      String[] levels = parent.split("/");
      for (String level : levels) {
        categoryView = categoryView.getChild(level);
      }
    }
    Map<String, SortedSet<String>> tree = CategoryUtils.getShortTree(categoryView);
    List<Result> results = new ArrayList<>(tree.size());
    for (Map.Entry<String, SortedSet<String>> entry : tree.entrySet()) {
      results.add(new Result(entry.getKey(), entry.getValue().size()));
    }
    Collections.sort(results);
    serializeMetadata(format, receiver, results);
  }
}
