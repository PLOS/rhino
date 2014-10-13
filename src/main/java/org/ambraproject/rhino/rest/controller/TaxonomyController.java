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

import com.google.common.base.Strings;
import org.ambraproject.models.Article;
import org.ambraproject.models.Category;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ClassificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller class for the taxonomy namespace.
 */
@Controller
public class TaxonomyController extends RestController {

  private static final String TAXONOMY_ROOT = "/taxonomy";
  private static final String TAXONOMY_NAMESPACE = TAXONOMY_ROOT + '/';
  private static final String TAXONOMY_TEMPLATE = TAXONOMY_NAMESPACE + "**";

  @Autowired
  private ClassificationService classificationService;

  @Autowired
  protected ArticleCrudService articleCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = TAXONOMY_TEMPLATE, method = RequestMethod.GET)
  public void readRoot(HttpServletRequest request, HttpServletResponse response,
                       @RequestParam(value = "journal", required = true) String journal)
      throws Exception {
    String parent = getFullPathVariable(request, true, TAXONOMY_NAMESPACE);
    if (!Strings.isNullOrEmpty(parent)) {
      parent = URLDecoder.decode(parent, "UTF-8");
    }
    classificationService.read(journal, parent).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = TAXONOMY_NAMESPACE + "flag/{action:add|remove}", method = RequestMethod.POST)
  public @ResponseBody Map<String,String> flagArticleCategory(
                       @RequestParam(value = "categoryTerm", required = true) String categoryTerm,
                       @RequestParam(value = "articleDoi", required = true) String articleDoi,
                       @RequestParam(value = "authId", required = true) String authId,
                       @PathVariable("action") String action)
          throws Exception {
    // TODO: we might want to optimize this by directly retrieving an article category collection in place of article instantiation
    Article article = articleCrudService.findArticleById(ArticleIdentity.create(articleDoi));
    for (Category category : article.getCategories().keySet()) {
      // if category matches the provided term, insert or delete an article category flag according to action provided in url
      // NOTE: a given category term (i.e. the final term in a full category path) may be present for more than one article category
      String[] terms = category.getPath().split("/");
      String articleCategoryTerm = terms[terms.length - 1];
      if (categoryTerm.contentEquals(articleCategoryTerm)) {
        if (action.contentEquals("remove")) {
          classificationService.deflagArticleCategory(article.getID(), category.getID(), authId);
        } else if (action.contentEquals("add")) {
          classificationService.flagArticleCategory(article.getID(), category.getID(), authId);
        }
      }
    }
    return new HashMap<>(); // ajax call expects returned data so provide an empty map for the body
  }

}
