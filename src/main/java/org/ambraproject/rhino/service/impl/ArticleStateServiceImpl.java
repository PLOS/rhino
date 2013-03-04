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

import org.ambraproject.models.Article;
import org.ambraproject.models.Syndication;
import org.ambraproject.rhino.content.ArticleState;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleStateService;
import org.ambraproject.service.article.NoSuchArticleIdException;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * {@inheritDoc}
 */
public class ArticleStateServiceImpl extends AmbraService implements ArticleStateService {

  /**
   * Helper method to set the syndication state for the appropriate target based on
   * the status property of the Syndication object.
   *
   * @param state ArticleState object that will be modified
   * @param syndication Syndication we are reading from
   */
  private void setSyndicationState(ArticleState state, Syndication syndication) {
    ArticleState.SyndicationTarget target
        = ArticleState.SyndicationTarget.valueOf(syndication.getTarget());
    ArticleState.SyndicationState syndicationState
        = ArticleState.SyndicationState.valueOf(syndication.getStatus());
    state.setSyndicationState(target, syndicationState);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void read(HttpServletResponse response, ArticleIdentity articleId, MetadataFormat format)
      throws IOException {
    Article article = loadArticle(articleId);
    ArticleState state = new ArticleState();
    state.setPublished(article.getState() == 0);
    List<Syndication> syndications;
    try {
      syndications = syndicationService.getSyndications(article.getDoi());
    } catch (NoSuchArticleIdException nsaide) {

      // Should never happen since we just loaded the article.
      throw new RuntimeException(nsaide);
    }
    for (Syndication syndication : syndications) {
      setSyndicationState(state, syndication);
    }

    assert format == MetadataFormat.JSON;
    writeJsonToResponse(response, state);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(ArticleIdentity articleId, ArticleState state) {
    Article article = loadArticle(articleId);
    article.setState(state.isPublished() ? 0 : 1);
    for (ArticleState.SyndicationTarget target : ArticleState.SyndicationTarget.values()) {

      // TODO: should we always re-attempt the syndication, as we do here, if it's
      // IN_PROGRESS?  Or base it on the Syndication.status of the appropriate target?
      // Not sure yet.
      if (state.getSyndicationState(target) == ArticleState.SyndicationState.IN_PROGRESS) {
        try {
          syndicationService.syndicate(article.getDoi(), target.toString());
        } catch (NoSuchArticleIdException nsaide) {

          // Should never happen since we just loaded the article.
          throw new RuntimeException(nsaide);
        }
      }

      // TODO: un-syndicate, if necessary.
    }
    hibernateTemplate.update(article);
  }

  private Article loadArticle(ArticleIdentity articleId) {
    Article result = (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", articleId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (result == null) {
      throw new RestClientException("Article not found: " + articleId.getIdentifier(),
          HttpStatus.NOT_FOUND);
    }
    return result;
  }
}
