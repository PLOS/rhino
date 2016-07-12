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

package org.ambraproject.rhino.rest.controller;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Category;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.OptionalLong;

/**
 * Controller class for the taxonomy namespace.
 */
@Controller
public class TaxonomyController extends RestController {

  private static final String TAXONOMY_ROOT = "/taxonomy";
  private static final String TAXONOMY_NAMESPACE = TAXONOMY_ROOT + '/';
  private static final Splitter TAXONOMY_PATH_SPLITTER = Splitter.on('/');

  @Autowired
  private TaxonomyService taxonomyService;

  @Autowired
  protected ArticleCrudService articleCrudService;

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = TAXONOMY_NAMESPACE + "flag/{action:add|remove}", method = RequestMethod.POST)
  public @ResponseBody Map<String,String> flagArticleCategory(
                       @RequestParam(value = "categoryTerm", required = true) String categoryTerm,
                       @RequestParam(value = "articleDoi", required = true) String articleDoi,
                       @RequestParam(value = "userId", required = false) String userId,
                       @PathVariable("action") String action)
          throws Exception {
    // TODO: we might want to optimize this by directly retrieving an article category collection in place of article instantiation
    Article article = null; // TODO

    OptionalLong userIdObj = (userId == null) ? OptionalLong.empty() : OptionalLong.of(Long.parseLong(userId));

    for (Category category : article.getCategories().keySet()) {
      // if category matches the provided term, insert or delete an article category flag according to action provided in url
      // NOTE: a given category term (i.e. the final term in a full category path) may be present for more than one article category
      String articleCategoryTerm = Iterables.getLast(TAXONOMY_PATH_SPLITTER.split(category.getPath()));
      if (categoryTerm.contentEquals(articleCategoryTerm)) {
        if (action.contentEquals("remove")) {
          taxonomyService.deflagArticleCategory(article.getID(), category.getCategoryId(), userIdObj);
        } else if (action.contentEquals("add")) {
          taxonomyService.flagArticleCategory(article.getID(), category.getCategoryId(), userIdObj);
        }
      }
    }
    return ImmutableMap.of(); // ajax call expects returned data so provide an empty map for the body
  }

}
