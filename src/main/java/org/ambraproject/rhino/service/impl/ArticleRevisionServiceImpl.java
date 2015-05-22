package org.ambraproject.rhino.service.impl;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.service.ArticleRevisionService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.plos.crepo.exceptions.NotFoundException;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.model.RepoVersionNumber;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.io.IOException;
import java.util.*;

public class ArticleRevisionServiceImpl extends AmbraService implements ArticleRevisionService {

  private RepoCollectionMetadata findCollectionFor(ArticleIdentity article) {
    String articleKey = article.getIdentifier();
    Optional<Integer> versionNumber = article.getVersionNumber();
    try {
      return versionNumber.isPresent()
          ? contentRepoService.getCollection(new RepoVersionNumber(articleKey, versionNumber.get()))
          : contentRepoService.getLatestCollection(articleKey);
    }catch (NotFoundException nfe){
      throw entityNotFound(nfe.getMessage() + ": " + articleKey);
    }
  }

  private ArticleRevision getLatestRevision(final String doi) {
    return hibernateTemplate.execute(new HibernateCallback<ArticleRevision>() {
      @Override
      public ArticleRevision doInHibernate(Session session) {
        Query query = session.createQuery("from ArticleRevision where doi=? order by revisionNumber desc");
        query.setString(0, doi);
        query.setMaxResults(1);
        return (ArticleRevision) DataAccessUtils.singleResult(query.list());
      }
    });
  }

  @Override
  public void createRevision(ArticleIdentity article, Integer revisionNumber) {
    String articleKey = article.getIdentifier();

    ArticleRevision revision;
    if (revisionNumber == null) {
      ArticleRevision latestRevision = getLatestRevision(articleKey); // TODO: Transaction safety
      int newRevisionNumber = (latestRevision == null) ? 1 : latestRevision.getRevisionNumber() + 1;

      revision = new ArticleRevision();
      revision.setDoi(articleKey);
      revision.setRevisionNumber(newRevisionNumber);
    } else {
      revision = (ArticleRevision) DataAccessUtils.uniqueResult(
          hibernateTemplate.find("from ArticleRevision where doi=? and revisionNumber=?",
              articleKey, revisionNumber));
      if (revision == null) {
        revision = new ArticleRevision();
        revision.setDoi(articleKey);
        revision.setRevisionNumber(revisionNumber);
      }
    }

    RepoCollectionMetadata collection = findCollectionFor(article);
    revision.setCrepoUuid(collection.getVersion().getUuid().toString());
    hibernateTemplate.persist(revision);
  }

  @Override
  public boolean deleteRevision(ArticleIdentity article, Integer revisionNumber) {
    ArticleRevision revision = (ArticleRevision) DataAccessUtils.uniqueResult(
        hibernateTemplate.find("from ArticleRevision where doi=? and revisionNumber=?",
            article.getIdentifier(), revisionNumber));
    if (revision == null) {
      return false;
    }
    hibernateTemplate.delete(revision);
    return true;
  }

  private String findRevisionUuid(String articleKey, int revisionNumber) {
    return (String) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(ArticleRevision.class)
            .setProjection(Projections.property("crepoUuid"))
            .add(Restrictions.eq("doi", articleKey))
            .add(Restrictions.eq("revisionNumber", revisionNumber))));
  }

  @Override
  public Integer findVersionNumber(ArticleIdentity article, int revisionNumber) {
    String articleKey = article.getIdentifier();
    String uuid = findRevisionUuid(articleKey, revisionNumber);
    if (uuid == null) {
      throw entityNotFound("Revision doesn't exist: " + article);
    }

    RepoCollectionMetadata collectionMetadata;
    try {
      collectionMetadata = contentRepoService.getCollection(RepoVersion.create(articleKey, uuid));
    } catch(NotFoundException nfe) {
      throw entityNotFound(nfe.getMessage() + ": " + articleKey);
    }

    return collectionMetadata.getVersionNumber().getNumber();
  }


  /**
   * Search a collection for an object in it with a given key.
   *
   * @param collection the collection to search
   * @param objectKey  the key of the object to search for
   * @return the metadata of the found object, or {@code null} if no object with the key is in the collection
   * @throws IllegalArgumentException if two or more objects in the collection have the given key
   */
  private RepoObjectMetadata findObjectInCollection(RepoCollectionMetadata collection, String objectKey) {
    RepoObjectMetadata found = null;
    for (RepoObjectMetadata objectMetadata : collection.getObjects()) {
      if (objectMetadata.getVersion().getKey().equals(objectKey)) {
        if (found != null) {
          throw new IllegalArgumentException("Multiple objects have key: " + objectKey);
        }
        found = objectMetadata;
      }
    }
    return found;
  }

  @Override
  public RepoObjectMetadata readFileVersion(ArticleIdentity articleIdentity, String fileKey) {
    RepoCollectionMetadata collection = findCollectionFor(articleIdentity);
    RepoObjectMetadata objectMetadata = findObjectInCollection(collection, fileKey);
    if (objectMetadata == null) throw entityNotFound("File not found: " + articleIdentity);
    return objectMetadata;
  }


  private static class RevisionVersionMapping {
    private final int versionNumber;
    private final Collection<Integer> revisionNumbers;

    public RevisionVersionMapping(int versionNumber) {
      Preconditions.checkArgument(versionNumber >= 0);
      this.versionNumber = versionNumber;
      this.revisionNumbers = new TreeSet<>();
    }
  }

  private static final Ordering<RevisionVersionMapping> ORDER_BY_VERSION_NUMBER = Ordering.natural().onResultOf(new Function<RevisionVersionMapping, Integer>() {
    @Override
    public Integer apply(RevisionVersionMapping input) {
      return input.versionNumber;
    }
  });

  /**
   * Describe the full list of back-end versions for one article, and the article revisions (if any) associated with
   * each version.
   *
   * @param articleIdentity
   * @return
   */
  @Override
  public Transceiver listRevisions(final ArticleIdentity articleIdentity) {
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        return fetchRevisions(articleIdentity);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  private Collection<?> fetchRevisions(ArticleIdentity articleIdentity) throws IOException {
    List<RepoCollectionMetadata> versions = contentRepoService.getCollectionVersions(articleIdentity.getIdentifier());
    Map<UUID, RevisionVersionMapping> mappings = Maps.newHashMapWithExpectedSize(versions.size());
    for (RepoCollectionMetadata version : versions) {
      RevisionVersionMapping mapping = new RevisionVersionMapping(version.getVersionNumber().getNumber());
      mappings.put(version.getVersion().getUuid(), mapping);
    }

    List<ArticleRevision> revisions = hibernateTemplate.find("from ArticleRevision where doi=?", articleIdentity.getIdentifier());
    if (revisions.isEmpty()) {
      throw entityNotFound("Revision doesn't exist: " + articleIdentity);
    }
    for (ArticleRevision revision : revisions) {
      RevisionVersionMapping mapping = mappings.get(UUID.fromString(revision.getCrepoUuid()));
      mapping.revisionNumbers.add(revision.getRevisionNumber());
    }

    return ORDER_BY_VERSION_NUMBER.immutableSortedCopy(mappings.values());
  }

  @Override
  public String getParentDoi(String doi) {
    doi = DoiBasedIdentity.asIdentifier(doi);
    return (String) DataAccessUtils.uniqueResult(hibernateTemplate.find(
        "select parentArticleDoi from DoiAssociation where doi=?", doi));
  }

  @Override
  public RepoObjectMetadata getObjectVersion(AssetIdentity assetIdentity, String repr, int revisionNumber) {
    String parentArticleDoi = getParentDoi(assetIdentity.getIdentifier());
    if (parentArticleDoi == null) throw entityNotFound("Unrecognized asset DOI");

    String parentArticleUuid = findRevisionUuid(parentArticleDoi, revisionNumber);
    if (parentArticleUuid == null) throw entityNotFound("Revision not found");

    RepoObjectMetadata repoObjectMetadata = null;

    try {
      RepoCollectionMetadata articleCollection = contentRepoService.getCollection(RepoVersion.create(parentArticleDoi, parentArticleUuid));
      Map<String, ?> articleMetadata = (Map<String, ?>) articleCollection.getJsonUserMetadata().get();
      Collection<Map<String, ?>> assets = (Collection<Map<String, ?>>) articleMetadata.get("assets");
      RepoVersion assetVersion = findAssetVersion(repr, assets, assetIdentity);

      repoObjectMetadata = contentRepoService.getRepoObjectMetadata(assetVersion);

    } catch(NotFoundException nfe) {
      entityNotFound(nfe.getMessage());
    }

    return repoObjectMetadata;
  }

  private static RepoVersion parseRepoVersion(Map<String, ?> versionJson) {
    return RepoVersion.create((String) versionJson.get("key"), (String) versionJson.get("uuid"));
  }

  private static RepoVersion findAssetVersion(String repr, Collection<Map<String, ?>> assets, AssetIdentity targetAssetId) {
    for (Map<String, ?> asset : assets) {
      AssetIdentity assetId = AssetIdentity.create((String) asset.get("doi"));
      if (assetId.equals(targetAssetId)) {
        Map<String, ?> assetObjects = (Map<String, ?>) asset.get("objects");
        Map<String, ?> assetRepr = (Map<String, ?>) assetObjects.get(repr);
        if (assetRepr == null) throw entityNotFound("repr not found");
        return parseRepoVersion(assetRepr);
      }
    }
    // We already match the asset DOI to this article in the DoiAssociation table,
    // but this is still possible if the asset appears in other revisions of the article but not this one.
    throw entityNotFound("Asset not in revision");
  }

  @Override
  public RepoObjectMetadata getManuscript(ArticleIdentity article) {
    RepoCollectionMetadata articleCollection = findCollectionFor(article);
    Map<String, ?> manuscript = (Map<String, ?>) ((Map<String, ?>) articleCollection.getJsonUserMetadata().get()).get("manuscript");
    RepoVersion manuscriptVersion = parseRepoVersion(manuscript);
    RepoObjectMetadata repoObjectMetadata;
    try {
      repoObjectMetadata = contentRepoService.getRepoObjectMetadata(manuscriptVersion);
    } catch (NotFoundException nfe){
      throw entityNotFound(nfe.getMessage() + ": " + manuscriptVersion);
    }
    return repoObjectMetadata;
  }

}
