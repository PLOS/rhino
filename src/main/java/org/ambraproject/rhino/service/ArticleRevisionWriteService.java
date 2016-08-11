package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;

/**
 * Service for <em>destructive</em> operations on persistent {@link org.ambraproject.rhino.model.ArticleRevision}
 * entities.
 * <p>
 * For read-only {@link org.ambraproject.rhino.model.ArticleRevision} services, see {@link ArticleCrudService}.
 */
public interface ArticleRevisionWriteService {

  public abstract void createRevision(ArticleRevisionIdentifier revisionId, ArticleIngestionIdentifier ingestionId);

  public abstract void updateRevision(ArticleRevisionIdentifier revisionId, ArticleIngestionIdentifier ingestionId);

  public abstract void deleteRevision(ArticleRevisionIdentifier revisionId);

}
