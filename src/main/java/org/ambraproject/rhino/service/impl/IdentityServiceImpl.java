package org.ambraproject.rhino.service.impl;

import com.google.common.base.Optional;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleRevisionService;
import org.ambraproject.rhino.service.IdentityService;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Created by lucia.masola on 5/13/15.
 */
public class IdentityServiceImpl implements IdentityService {

  @Autowired
  private ArticleRevisionService articleRevisionService;

  public ArticleIdentity parseArticleId(String articleId, Integer versionNumber, Integer revisionNumber) {
    if (revisionNumber == null) {
      return new ArticleIdentity(articleId, Optional.fromNullable(versionNumber), Optional.<String>absent());
    } else {
      int revisionVersionNumber = articleRevisionService.findVersionNumber(ArticleIdentity.create(articleId), revisionNumber);
      if (versionNumber != null && versionNumber != revisionVersionNumber) {
        String message = String.format("Mismatch between version and revision " +
                "(provided v=%d&r=%d; correct revision for v=%d is r=%d)",
            versionNumber, revisionNumber, versionNumber, revisionVersionNumber);
        throw new RestClientException(message, HttpStatus.NOT_FOUND);
      }
      return new ArticleIdentity(articleId, Optional.of(revisionVersionNumber), Optional.<String>absent());
    }
  }

  public AssetIdentity parseAssetId(ArticleIdentity parentArticleId, DoiBasedIdentity assetId, String fileType, Integer revisionNumber){

    if (parentArticleId == null) {
      throw new RestClientException("Asset ID not mapped to article", HttpStatus.NOT_FOUND);
    }
    Integer versionNumber = parentArticleId.getVersionNumber().isPresent() ? parentArticleId.getVersionNumber().get() : null;
    parentArticleId = parseArticleId(parentArticleId.getIdentifier(), versionNumber, revisionNumber); // apply revision

    // get the userMeta of the article
    RepoCollectionMetadata articleMetadata = articleRevisionService.findCollectionFor(parentArticleId);

    // get the uuid of the asset
    Map<String, Object> userMetadata = (Map<String, Object>) articleMetadata.getJsonUserMetadata().get();

    return getAssetUUIDFromUserMetadata(userMetadata, assetId, fileType);

  }

  protected AssetIdentity getAssetUUIDFromUserMetadata(Map<String,Object> userMetadata, DoiBasedIdentity assetId, String fileType){

    Map<String, Object> assets = (Map<String, Object>) userMetadata.get("assets");
    Map<String, Object>  assetInfo = (Map<String, Object>)  assets.get(assetId.getIdentifier());
    Map<String, Object> assetsFiles = (Map<String, Object>) assetInfo.get("files");
    String uuid = (String)((Map<String, Object>) assetsFiles.get(fileType)).get("uuid");
    String id = fileType + "/" + assetId.getIdentifier();
    return new AssetIdentity(id, Optional.<Integer>absent(), Optional.fromNullable(uuid));

  }

}
