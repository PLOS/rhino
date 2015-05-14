package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;

/**
 * Created by lucia.masola on 5/13/15.
 */
public interface IdentityService {

  ArticleIdentity parseArticleId(String id, Integer versionNumber, Integer revisionNumber);

  AssetIdentity parseAssetId(ArticleIdentity parentArticleId, DoiBasedIdentity assetId, String fileType,Integer revisionNumber);

}
