/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.admin.controller;

import org.ambraproject.models.Article;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Does a sanity check on the admin application's ability to access Ambra's models and database.
 * <p/>
 * This should be replaced with a real home page when this project is out of development.
 */
@Controller
public class DemoController {

  @Autowired
  private HibernateTemplate hibernateTemplate;

  /**
   * Populate the page with some sample data from the Spring framework and the persistence layer.
   */
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public String demo(Locale locale, Model model) {
    model.addAttribute("clientLocale", locale.toString());
    DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
    String formattedDate = dateFormat.format(new Date());
    model.addAttribute("serverTime", formattedDate);

    List<?> dois = hibernateTemplate.findByCriteria(DetachedCriteria
        .forClass(Article.class)
        .setProjection(Projections.property("doi"))
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
    );
    model.addAttribute("articleCount", dois.size());
    model.addAttribute("articleDoiList", dois);
    return "demo";
  }

}
