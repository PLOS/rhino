package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.model.ArticleRevision;

/**
 * Service for <em>destructive</em> operations on persistent {@link org.ambraproject.rhino.model.ArticleRevision}
 * entities.
 * <p>
 * For read-only {@link org.ambraproject.rhino.model.ArticleRevision} services, see {@link ArticleCrudService}.
 */
public interface ArticleRevisionWriteService {

  /**
   * Create a new revision. The created revision points at the supplied ingestion and is assigned an automatically
   * incremented revision number. If no other revisions of the article exist, it is revision 1.
   *
   * @param ingestionId the ingestion to expose as the revision
   * @return the created object
   * @throws org.ambraproject.rhino.rest.RestClientException if the identified ingestion does not exist
   */
  public abstract ArticleRevision createRevision(ArticleIngestionIdentifier ingestionId);

  /**
   * Write a revision with a given number. A new revision is created if the given number doesn't already exist for this
   * article; otherwise, the revision is overwritten.
   *
   * @param revisionId  the identity of the revision to write (which may or may not already exist)
   * @param ingestionId the ingestion to expose as the revision
   * @return the created or updated object
   * @throws IllegalArgumentException                        if {@code revisionId} and {@code ingestionId} do not belong
   *                                                         to the same article
   * @throws org.ambraproject.rhino.rest.RestClientException if the identified ingestion does not exist
   */
  public abstract ArticleRevision writeRevision(ArticleRevisionIdentifier revisionId, ArticleIngestionIdentifier ingestionId);

  /**
   * Delete a revision.
   *
   * @param revisionId the identity of the revision to delete
   * @throws org.ambraproject.rhino.rest.RestClientException if the identified revision does not exist
   */
  public abstract void deleteRevision(ArticleRevisionIdentifier revisionId);

}
