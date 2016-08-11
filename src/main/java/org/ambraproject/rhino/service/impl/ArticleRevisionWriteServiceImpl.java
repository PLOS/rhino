package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleRevisionWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class ArticleRevisionWriteServiceImpl implements ArticleRevisionWriteService {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private HibernateTemplate hibernateTemplate;

  @Override
  public void createRevision(ArticleRevisionIdentifier revisionId, ArticleIngestionIdentifier ingestionId) {
    Preconditions.checkArgument(revisionId.getArticleIdentifier().equals(ingestionId.getArticleIdentifier()));
    if (articleCrudService.getRevision(revisionId).isPresent()) {
      throw new RestClientException("Revision exists", HttpStatus.BAD_REQUEST);
    }

    ArticleIngestion ingestion = articleCrudService.readIngestion(ingestionId);
    ArticleRevision revision = new ArticleRevision();
    revision.setIngestion(ingestion);
    revision.setRevisionNumber(revisionId.getRevision());
    hibernateTemplate.save(revision);
  }

  @Override
  public void updateRevision(ArticleRevisionIdentifier revisionId, ArticleIngestionIdentifier ingestionId) {
    Preconditions.checkArgument(revisionId.getArticleIdentifier().equals(ingestionId.getArticleIdentifier()));
    ArticleRevision revision = articleCrudService.readRevision(revisionId);
    ArticleIngestion ingestion = articleCrudService.readIngestion(ingestionId);
    revision.setIngestion(ingestion);
    hibernateTemplate.update(revision);
  }

  @Override
  public void deleteRevision(ArticleRevisionIdentifier revisionId) {
    ArticleRevision revision = articleCrudService.readRevision(revisionId);
    hibernateTemplate.delete(revision);
  }

}
