/*
 * Copyright (c) 2006-2013 by Public Library of Science
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

package org.ambraproject.rhino.service.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.ambraproject.models.Article;
import org.ambraproject.models.Pingback;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.service.PingbackReadService;
import org.ambraproject.rhino.util.response.EntityCollectionTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticlePingbackView;
import org.ambraproject.rhino.view.article.ArticleViewList;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class PingbackReadServiceImpl extends AmbraService implements PingbackReadService {

  private static Function<Object[], ArticlePingbackView> AS_VIEW = new Function<Object[], ArticlePingbackView>() {
    @Override
    public ArticlePingbackView apply(Object[] input) {
      String doi = (String) input[0];
      String title = (String) input[1];
      String url = (String) input[2];
      Long pingbackCount = (Long) input[3];
      Date mostRecentPingback = (Date) input[4];
      return new ArticlePingbackView(doi, title, url, pingbackCount, mostRecentPingback);
    }
  };

  @Override
  public Transceiver listByArticle(OrderBy orderBy) throws IOException {
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null; // Unsupported for now
      }

      @Override
      protected Object getData() throws IOException {
        // Here is what this is intended to do, in SQL:
        //  SELECT article.doi, article.title, pb.count, pb.mostRecent
        //  FROM article INNER JOIN
        //    (SELECT COUNT(url) AS count, MAX(created) AS mostRecent, articleID
        //     FROM pingback GROUP BY articleId) AS pb
        //  ON article.articleID = pb.articleID
        //  ORDER BY pb.mostRecent DESC;

        List<Object[]> results = hibernateTemplate.find(""
                + "select distinct a.doi, a.title, a.url, "
                + "  (select count(*) from Pingback where articleID = a.ID) as pingbackCount, "
                + "  (select max(created) from Pingback where articleID = a.ID) as mostRecent " // TODO Eliminate duplication?
                + "from Pingback as p, Article as a where p.articleID = a.ID "
                + "order by mostRecent desc "
        );
        List<ArticlePingbackView> resultView = Lists.transform(results, AS_VIEW);
        return new ArticleViewList(resultView);
      }
    };
  }

  @Override
  public Transceiver read(final ArticleIdentity article) throws IOException {
    return new EntityCollectionTransceiver<Pingback>() {
      @Override
      protected Collection<? extends Pingback> fetchEntities() {
        return loadPingbacks(article);
      }

      @Override
      protected Object getView(Collection<? extends Pingback> pingbacks) {
        return pingbacks;
      }
    };
  }

  @Override
  public List<Pingback> loadPingbacks(Article article) {
    return loadPingbacks(article.getID());
  }

  @Override
  public List<Pingback> loadPingbacks(ArticleIdentity article) {
    Long articleId = (Long) DataAccessUtils.uniqueResult(hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Article.class)
            .add(Restrictions.eq("doi", article.getKey()))
            .setProjection(Projections.property("ID"))
    ));
    if (articleId == null) {
      throw reportNotFound(article, "article");
    }
    return loadPingbacks(articleId);
  }

  private List<Pingback> loadPingbacks(long articleId) {
    return hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Pingback.class)
            .add(Restrictions.eq("articleID", articleId))
            .addOrder(Order.desc("created"))
    );
  }

}
