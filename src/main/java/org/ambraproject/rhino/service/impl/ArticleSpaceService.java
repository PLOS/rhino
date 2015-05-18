package org.ambraproject.rhino.service.impl;

import com.google.common.base.Optional;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoVersionNumber;

abstract class ArticleSpaceService extends AmbraService {

  protected RepoCollectionMetadata fetchArticleCollection(ArticleIdentity id) {
    String identifier = id.getIdentifier();
    Optional<Integer> versionNumber = id.getVersionNumber();
    RepoCollectionMetadata collection;
    if (versionNumber.isPresent()) {
      collection = contentRepoService.getCollection(new RepoVersionNumber(identifier, versionNumber.get()));
    } else {
      collection = contentRepoService.getLatestCollection(identifier);
    }
    return collection;
  }

}
