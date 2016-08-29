package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.ArticleTable;
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

    ArticleTable article = ingestion.getArticle();
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
    ArticleTable article = revision.getIngestion().getArticle();

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
