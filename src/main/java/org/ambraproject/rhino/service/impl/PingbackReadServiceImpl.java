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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.ambraproject.models.Article;
import org.ambraproject.models.Pingback;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.service.PingbackReadService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;

import java.io.IOException;
import java.util.List;

public class PingbackReadServiceImpl extends AmbraService implements PingbackReadService {

  private static class ArticleListView {
    private final Object doi;
    private final Object title;
    private final Object articleUrl;
    private final Object pingbackCount;
    private final Object mostRecentPingback;

    private ArticleListView(Object[] queryResult) {
      this.doi = queryResult[0];
      this.title = queryResult[1];
      this.articleUrl = queryResult[2];
      this.pingbackCount = queryResult[3];
      this.mostRecentPingback = queryResult[4];
    }
  }

  private static Function<Object[], ArticleListView> AS_VIEW = new Function<Object[], ArticleListView>() {
    @Override
    public ArticleListView apply(Object[] input) {
      return new ArticleListView(input);
    }
  };

  @Override
  public void listByArticle(ResponseReceiver receiver, MetadataFormat format, OrderBy orderBy) throws IOException {
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
    List<ArticleListView> resultView = Lists.transform(results, AS_VIEW);
    writeJson(receiver, resultView);
  }

  @Override
  public void read(ResponseReceiver receiver, ArticleIdentity article, MetadataFormat format) throws IOException {
    Preconditions.checkNotNull(article);
    long articleId = DataAccessUtils.longResult(hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Article.class)
            .add(Restrictions.eq("doi", article.getKey()))
            .setProjection(Projections.property("ID"))));
    List<?> results = hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Pingback.class)
            .add(Restrictions.eq("articleID", articleId))
            .addOrder(Order.desc("created"))
    );
    writeJson(receiver, results);
  }

  private static ImmutableMap<String, Object> view(Article article, Pingback pingback) {
    ImmutableMap.Builder<String, Object> view = ImmutableMap.<String, Object>builder();
    view.put("articleDoi", article.getDoi())
        .put("url", pingback.getUrl())
        .put("title", pingback.getTitle())
        .put("created", pingback.getCreated())
        .put("lastModified", pingback.getLastModified());
    return view.build();
  }

}
