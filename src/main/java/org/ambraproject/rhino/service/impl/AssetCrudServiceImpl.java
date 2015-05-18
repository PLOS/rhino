/*
 * Copyright (c) 2006-2012 by Public Library of Science
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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.model.DoiAssociation;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.WriteResult;
import org.ambraproject.rhino.util.response.EntityCollectionTransceiver;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleVisibility;
import org.ambraproject.rhino.view.asset.groomed.GroomedImageView;
import org.ambraproject.rhino.view.asset.groomed.UncategorizedAssetException;
import org.ambraproject.rhino.view.asset.raw.RawAssetFileCollectionView;
import org.ambraproject.rhino.view.asset.raw.RawAssetFileView;
import org.ambraproject.rhino.view.internal.RepoVersionRepr;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AssetCrudServiceImpl extends ArticleSpaceService implements AssetCrudService {

  private static final Logger log = LoggerFactory.getLogger(AssetCrudServiceImpl.class);

  private boolean assetExistsAt(DoiBasedIdentity id) {
    DetachedCriteria criteria = DetachedCriteria.forClass(ArticleAsset.class)
        .add(Restrictions.eq("doi", id.getKey()));
    return exists(criteria);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WriteResult<ArticleAsset> upload(InputStream file, AssetFileIdentity assetFileId)
      throws IOException {
    List<ArticleAsset> assets = (List<ArticleAsset>) hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", assetFileId.getKey()))
    );

    // Set up the Hibernate entity that we want to modify
    ArticleAsset assetToPersist = createAssetToAdd(assetFileId, assets);

    /*
     * Pull the data into memory and write it to the file store. Must buffer the whole thing in memory, because we need
     * to know the final size before we can open a stream to the file store. Also, we need to measure the size anyway
     * to record as an asset field.
     */
    byte[] assetData = readClientInput(file);
    write(assetData, assetFileId);

    // Set the asset entity's file-specific fields
    assetToPersist.setExtension(assetFileId.getFileExtension());
    assetToPersist.setContentType(assetFileId.inferContentType().toString());
    assetToPersist.setSize(assetData.length);

    // Persist to the database
    if (assets.contains(assetToPersist)) {
      hibernateTemplate.update(assetToPersist);
    } else {
      saveAssetForcingParentArticle(assetToPersist);
    }

    // Return the result
    return new WriteResult<>(assetToPersist, WriteResult.Action.CREATED);
  }

  /**
   * Examine a persistent collection of existing asset entities and select or create one to represent a new file upload.
   * If there is already a file-less asset with a matching DOI, this method finds and returns it. Else, it creates a new
   * asset with the article-defined data initialized and modifies the given collection by adding it.
   * <p/>
   * It also does two kinds of data validation. If the DOI-and-extension pair being created already exists, throw a
   * client error. If a file-less asset coexists with one or more assets that have file data, log a warning because this
   * should not be possible.
   *
   * @param idToCreate     the identity of the uploaded asset file that needs a new asset entity
   * @param existingAssets the collection of existing assets that have the same DOI as the new asset entity
   * @return the asset entity to be modified and persisted for this file upload
   * @throws RestClientException if the uploaded file collides with an existing file
   */
  private static ArticleAsset createAssetToAdd(final AssetFileIdentity idToCreate,
                                               final Collection<ArticleAsset> existingAssets) {
    final String keyToCreate = idToCreate.getKey();
    ArticleAsset newAsset = null;
    boolean creatingNewEntity = false;
    for (ArticleAsset existingAsset : existingAssets) {
      if (!keyToCreate.equals(existingAsset.getDoi())) {
        throw new IllegalArgumentException("Should have only assets with the same DOI");
      }
      if (!AssetIdentity.hasFile(existingAsset)) {
        // This asset is uninitialized.
        if (newAsset != null) {
          // We previously matched an asset that had a file, and expected not to find this file-less one.
          complainAboutUninitializedAsset(idToCreate);
        }
        // We will modify the file-less asset to give it a file. Won't insert a new asset object.
        newAsset = existingAsset;
        creatingNewEntity = false;
      } else if (idToCreate.equals(AssetFileIdentity.from(existingAsset))) {
        String message = "Asset already exists: " + idToCreate;
        throw new RestClientException(message, HttpStatus.METHOD_NOT_ALLOWED);
      } else if (newAsset == null) {
        // Found an existing, initialized asset with a matching DOI.
        // Will insert a new asset object to represent this file, copying the old one's article-defined fields.
        newAsset = copyArticleFields(existingAsset);
        creatingNewEntity = true;
      } else if (!creatingNewEntity) {
        // We previously matched a file-less asset, and expected not to find this one that has a file.
        complainAboutUninitializedAsset(idToCreate);
      }
    }

    if (newAsset == null) {
      String message = String.format(
          "No asset found with DOI=\"%s\" (must appear in an article that has been ingested)",
          idToCreate.getIdentifier());
      throw new RestClientException(message, HttpStatus.METHOD_NOT_ALLOWED);
    }
    return newAsset;
  }

  private static void complainAboutUninitializedAsset(AssetFileIdentity identity) {
    log.warn("Illegal data state: An uninitialized ArticleAsset entity coexists "
        + "with one or more asset files on DOI = {}", identity.getIdentifier());
  }

  /**
   * Copy all fields defined by article XML into a new asset and return it.
   *
   * @param oldAsset the asset to copy from
   * @return the new asset
   */
  private static ArticleAsset copyArticleFields(ArticleAsset oldAsset) {
    Preconditions.checkNotNull(oldAsset);
    ArticleAsset newAsset = new ArticleAsset();
    newAsset.setDoi(oldAsset.getDoi());
    newAsset.setTitle(oldAsset.getTitle());
    newAsset.setDescription(oldAsset.getDescription());
    newAsset.setContextElement(oldAsset.getContextElement());
    return newAsset;
  }

  /**
   * Efficiently save an asset. The argument must be a new asset that hasn't been persisted to Hibernate yet that shares
   * a DOI with at least one other existing asset. If the parent is {@code article}, then this method is equivalent to
   * <pre>
   *   article.getAssets().add(asset);
   *   hibernateTemplate.update(article);
   * </pre>
   * But, it uses raw SQL to hack around the costs of doing it that way.
   *
   * @param asset a new asset
   */
  private void saveAssetForcingParentArticle(ArticleAsset asset) {
    final String assetDoi = asset.getDoi();
    Preconditions.checkArgument(!Strings.isNullOrEmpty(assetDoi));

    Object[] result = (Object[]) DataAccessUtils.uniqueResult(
        hibernateTemplate.execute(new HibernateCallback<List<?>>() {
          @Override
          public List<?> doInHibernate(Session session) throws HibernateException, SQLException {
            // Find the parent article, and the max sort order for all assets with that parent (not necessarily same asset DOI)
            SQLQuery query = session.createSQLQuery(""
                + "SELECT forArticle.articleID, MAX(forArticle.sortOrder) FROM "
                + "  articleAsset forArticle INNER JOIN articleAsset forDoi "
                + "  ON forArticle.articleID = forDoi.articleID "
                + "WHERE forDoi.doi = :assetDoi "
                + "GROUP BY forArticle.articleID");

            // TODO: Consider this query instead
            // It seems to do the same thing and is simpler, but probably performs worse.
            //   SELECT articleID, MAX(sortOrder)
            //   FROM articleAsset WHERE articleID =
            //     (SELECT DISTINCT articleID FROM articleAsset WHERE doi = :assetDoi)
            //   GROUP BY articleID

            query.setParameter("assetDoi", assetDoi);
            return query.list();
          }
        })
    );
    final BigInteger parentArticleId = Preconditions.checkNotNull((BigInteger) result[0]);
    int maxSortOrder = (Integer) result[1];
    final int newSortOrder = maxSortOrder + 1;

    hibernateTemplate.save(asset);
    final Long newAssetId = Preconditions.checkNotNull(asset.getID());

    hibernateTemplate.execute(new HibernateCallback<Integer>() {
      @Override
      public Integer doInHibernate(Session session) throws HibernateException, SQLException {
        SQLQuery query = session.createSQLQuery(""
            // Insert the new asset into the article's "assets" list as Hibernate would.
            + "UPDATE articleAsset "
            + "SET articleID = :parentArticleId, sortOrder = :newSortOrder "
            + "WHERE articleAssetID = :newAssetId");
        query.setParameter("parentArticleId", parentArticleId);
        query.setParameter("newSortOrder", newSortOrder);
        query.setParameter("newAssetId", newAssetId);
        return query.executeUpdate();
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void overwrite(InputStream fileContent, AssetFileIdentity id) throws IOException {
    ArticleAsset asset = (ArticleAsset) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(ArticleAsset.class)
                .add(Restrictions.eq("doi", id.getKey()))
                .add(Restrictions.eq("extension", id.getFileExtension()))
        ));
    if (asset == null) {
      throw new RestClientException("Asset not found at: " + id, HttpStatus.NOT_FOUND);
    }

    byte[] assetData = readClientInput(fileContent);
    write(assetData, id);
  }

  @Override
  public RepoObjectMetadata read(AssetIdentity assetIdentity, ArticleIdentity parentArticle, String fileType) {
    RepoCollectionMetadata articleCollection = fetchArticleCollection(parentArticle);
    Map<String, Object> articleMetadata = (Map<String, Object>) articleCollection.getJsonUserMetadata().get();

    Map<String, Object> assets = (Map<String, Object>) articleMetadata.get("assets");
    Map<String, Object> assetEntry = (Map<String, Object>) assets.get(assetIdentity.getIdentifier());
    if (assetEntry == null) {
      String message = String.format("Article (%s) does not have asset: %s", parentArticle, assetIdentity);
      throw new RestClientException(message, HttpStatus.NOT_FOUND);
    }

    Map<String, Object> files = (Map<String, Object>) assetEntry.get("files");
    Map<String, Object> fileId = (Map<String, Object>) files.get(fileType);
    if (fileId == null) {
      String message = String.format("Asset (%s) of article (%s) does not have file type: \"%s\". Files types are: %s",
          assetIdentity, parentArticle, fileType, files.keySet());
      throw new RestClientException(message, HttpStatus.NOT_FOUND);
    }
    RepoVersion fileVersion = RepoVersionRepr.read(fileId);

    return contentRepoService.getRepoObjectMetadata(fileVersion);
  }

  /**
   * Find article asset objects.
   *
   * @param id the asset ID
   * @return a collection of all {@code ArticleAsset} objects whose DOI matches the ID
   * @throws RestClientException if no asset files exist with that ID
   */
  private abstract class ArticleAssetsRetriever extends EntityCollectionTransceiver<ArticleAsset> {
    private final AssetIdentity id;

    protected ArticleAssetsRetriever(AssetIdentity id) {
      this.id = Preconditions.checkNotNull(id);
    }

    @Override
    protected final Calendar getLastModifiedDate() throws IOException {
      Date lastModified = (Date) DataAccessUtils.uniqueResult(hibernateTemplate.find(
          "select max(lastModified) from ArticleAsset where doi = ?", id.getKey()));
      return (lastModified == null) ? null : copyToCalendar(lastModified);
    }

    @Override
    protected final Collection<? extends ArticleAsset> fetchEntities() {
      @SuppressWarnings("unchecked") List<ArticleAsset> assets = ((List<ArticleAsset>)
          hibernateTemplate.findByCriteria(DetachedCriteria
                  .forClass(ArticleAsset.class)
                  .add(Restrictions.eq("doi", id.getKey()))
                  .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
          ));
      if (assets.isEmpty()) {
        throw reportNotFound(id);
      }
      return assets;
    }
  }

  @Override
  public Transceiver readMetadata(final AssetIdentity id)
      throws IOException {
    return new ArticleAssetsRetriever(id) {
      @Override
      protected Object getView(Collection<? extends ArticleAsset> assets) {
        ArticleVisibility parentArticle = findArticleFor(id);
        return new RawAssetFileCollectionView(assets, parentArticle);
      }
    };
  }

  @Override
  public Transceiver readFigureMetadata(final AssetIdentity id)
      throws IOException {
    return new ArticleAssetsRetriever(id) {
      @Override
      protected Object getView(Collection<? extends ArticleAsset> assets) {
        GroomedImageView figureView;
        try {
          figureView = GroomedImageView.create(assets);
        } catch (UncategorizedAssetException e) {
          String message = "Not a figure asset: " + id.getIdentifier();
          throw new RestClientException(message, HttpStatus.BAD_REQUEST, e);
        }
        figureView.setParentArticle(findArticleFor(figureView.getIdentity()));
        return figureView;
      }
    };
  }

  @Override
  public Transceiver readFileMetadata(final AssetFileIdentity id)
      throws IOException {
    return new EntityTransceiver<ArticleAsset>() {
      @Override
      protected ArticleAsset fetchEntity() {
        @SuppressWarnings("unchecked") List<ArticleAsset> assets = ((List<ArticleAsset>)
            hibernateTemplate.findByCriteria(DetachedCriteria
                    .forClass(ArticleAsset.class)
                    .add(Restrictions.eq("doi", id.getKey()))
                    .add(Restrictions.eq("extension", id.getFileExtension()))
                    .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
            ));
        if (assets.isEmpty()) {
          throw reportNotFound(id);
        }
        return DataAccessUtils.requiredUniqueResult(assets);
      }

      @Override
      protected Object getView(ArticleAsset asset) {
        ArticleVisibility parentArticle = findArticleFor(id.forAsset());
        return new RawAssetFileView(asset, parentArticle);
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(AssetFileIdentity assetId) {
    ArticleAsset asset = (ArticleAsset) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
                .forClass(ArticleAsset.class)
                .add(Restrictions.eq("doi", assetId.getKey()))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (asset == null) {
      throw reportNotFound(assetId);
    }

    hibernateTemplate.delete(asset);
    delete(assetId);
  }

  private ArticleVisibility findArticleFor(AssetIdentity id) {
    Object[] articleResult = (Object[]) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.find(
            "select distinct a.doi, a.state "
                + "from Article a join a.assets b "
                + "where b.doi = ?",
            id.getKey()
        ));
    String articleDoi = (String) articleResult[0];
    Integer articleState = (Integer) articleResult[1];
    if (articleDoi == null) {
      throw new RestClientException("Asset not found for: " + id.getIdentifier(), HttpStatus.NOT_FOUND);
    }

    List<Journal> journalResult = hibernateTemplate.find("select journals from Article where doi = ?", articleDoi);

    return new ArticleVisibility(articleDoi, articleState, journalResult);
  }

  @Override
  public ArticleIdentity getParentArticle(DoiBasedIdentity identity) {
    String parentArticleDoi = (String) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(DoiAssociation.class)
            .setProjection(Projections.property("parentArticleDoi"))
            .add(Restrictions.eq("doi", identity.getIdentifier()))));
    return (parentArticleDoi == null) ? null : ArticleIdentity.create(parentArticleDoi);
  }

}
