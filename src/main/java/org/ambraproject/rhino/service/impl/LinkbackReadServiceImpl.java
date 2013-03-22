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
import org.ambraproject.models.Linkback;
import org.ambraproject.models.Trackback;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.service.LinkbackReadService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class LinkbackReadServiceImpl extends AmbraService implements LinkbackReadService {

  private static class ArticleListView {
    private final String doi;
    private final String title;
    private final long linkbackCount;
    private final Date mostRecentLinkback;

    private ArticleListView(Object[] queryResult) {
      this.doi = (String) queryResult[0];
      this.title = (String) queryResult[1];
      this.linkbackCount = (Long) queryResult[2];
      this.mostRecentLinkback = (Date) queryResult[3];
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
    List<Object[]> results = hibernateTemplate.find(""
        + "select a.doi, a.title, count(distinct p.ID) as linkbackCount, max(p.created) as mostRecentLinkback "
        + "from Pingback as p, Article as a where p.articleID = a.ID order by mostRecentLinkback desc");
    writeJson(receiver, Lists.transform(results, AS_VIEW));
  }

  @Override
  public void read(ResponseReceiver receiver, ArticleIdentity article, MetadataFormat format) throws IOException {
    Preconditions.checkNotNull(article);
    long articleId = DataAccessUtils.longResult(hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Article.class)
            .add(Restrictions.eq("doi", article.getKey()))
            .setProjection(Projections.property("ID"))));
    List<?> results = hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Linkback.class)
            .add(Restrictions.eq("articleID", articleId))
            .addOrder(Order.asc("created"))
    );
    writeJson(receiver, results);
  }

  private static ImmutableMap<String, Object> view(Article article, Linkback linkback) {
    ImmutableMap.Builder<String, Object> view = ImmutableMap.<String, Object>builder();
    view.put("articleDoi", article.getDoi())
        .put("url", linkback.getUrl())
        .put("title", linkback.getTitle())
        .put("created", linkback.getCreated())
        .put("lastModified", linkback.getLastModified());
    if (linkback instanceof Trackback) {
      Trackback trackback = (Trackback) linkback;
      view.put("blogName", trackback.getBlogName())
          .put("excerpt", trackback.getExcerpt());
    }
    return view.build();
  }

}
