/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleRevisionWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.Optional;

public class ArticleRevisionWriteServiceImpl implements ArticleRevisionWriteService {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private HibernateTemplate hibernateTemplate;

  @Override
  public ArticleRevision createRevision(ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion ingestion = articleCrudService.readIngestion(ingestionId);
    Optional<ArticleRevision> previousLatest = articleCrudService.getLatestRevision(ingestion.getArticle());
    int newRevisionNumber = 1 + previousLatest.map(ArticleRevision::getRevisionNumber).orElse(0);

    ArticleRevision revision = new ArticleRevision();
    revision.setIngestion(ingestion);
    revision.setRevisionNumber(newRevisionNumber);
    hibernateTemplate.save(revision);

    refreshForLatestRevision(revision);

    return revision;
  }

  @Override
  public ArticleRevision writeRevision(ArticleRevisionIdentifier revisionId, ArticleIngestionIdentifier ingestionId) {
    Preconditions.checkArgument(revisionId.getArticleIdentifier().equals(ingestionId.getArticleIdentifier()));
    ArticleIngestion ingestion = articleCrudService.readIngestion(ingestionId);

    Article article = ingestion.getArticle();
    Optional<ArticleRevision> previousLatest = articleCrudService.getLatestRevision(article);

    ArticleRevision newRevision = articleCrudService.getRevision(revisionId)
        .orElseGet(() -> {
          ArticleRevision revision = new ArticleRevision();
          revision.setRevisionNumber(revisionId.getRevision());
          return revision;
        });
    newRevision.setIngestion(ingestion);
    hibernateTemplate.saveOrUpdate(newRevision);

    if (!previousLatest.isPresent() || previousLatest.get().getRevisionNumber() <= newRevision.getRevisionNumber()) {
      refreshForLatestRevision(newRevision);
    }

    return newRevision;
  }

  @Override
  public void deleteRevision(ArticleRevisionIdentifier revisionId) {
    ArticleRevision revision = articleCrudService.readRevision(revisionId);
    Article article = revision.getIngestion().getArticle();

    ArticleRevision latestRevision = articleCrudService.getLatestRevision(article)
        .orElseThrow(RuntimeException::new); // should be guaranteed to exist because at least one revision exists
    boolean deletingLatest = latestRevision.equals(revision);

    hibernateTemplate.delete(revision);

    if (deletingLatest) {
      articleCrudService.getLatestRevision(article)
          .ifPresent(this::refreshForLatestRevision);
      // else, we deleted the only revision
    }
  }

  private void refreshForLatestRevision(ArticleRevision newlyLatestRevision) {
    articleCrudService.refreshArticleRelationships(newlyLatestRevision);
  }

}
