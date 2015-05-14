package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.util.response.Transceiver;
import org.plos.crepo.model.RepoObjectMetadata;

import java.io.IOException;

public interface ArticleRevisionService {

  void createRevision(ArticleIdentity article, Integer revisionNumber) throws IOException;

  boolean deleteRevision(ArticleIdentity article, Integer revisionNumber);

  Integer findVersionNumber(ArticleIdentity article, int revisionNumber);

  RepoObjectMetadata readFileVersion(ArticleIdentity articleIdentity, String fileKey);

  Transceiver listRevisions(ArticleIdentity articleIdentity);

  String getParentDoi(String doi);

  RepoObjectMetadata getObjectVersion(AssetIdentity assetIdentity, String repr, int revisionNumber);

  RepoObjectMetadata getManuscript(ArticleIdentity article);

}
