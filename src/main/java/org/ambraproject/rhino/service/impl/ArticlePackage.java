package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import org.hibernate.SQLQuery;
import org.plos.crepo.model.RepoCollectionList;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class ArticlePackage {

  private final ScholarlyWork articleWork;
  private final ImmutableList<ScholarlyWork> assetWorks;

  ArticlePackage(ScholarlyWork articleWork, List<ScholarlyWork> assetWorks) {
    this.articleWork = Objects.requireNonNull(articleWork);
    this.assetWorks = ImmutableList.copyOf(assetWorks);
  }


  private static class PersistedWork {
    private final ScholarlyWork scholarlyWork;
    private final RepoCollectionList result;

    private PersistedWork(ScholarlyWork scholarlyWork, RepoCollectionList result) {
      this.scholarlyWork = Objects.requireNonNull(scholarlyWork);
      this.result = Objects.requireNonNull(result);
    }
  }

  public void persist(HibernateTemplate hibernateTemplate, ContentRepoService contentRepoService) {
    RepoCollectionList persistedArticle = articleWork.persistToCrepo(contentRepoService);

    List<PersistedWork> persistedAssets = new ArrayList<>();
    for (ScholarlyWork assetWork : assetWorks) {
      RepoCollectionList persistedAsset = assetWork.persistToCrepo(contentRepoService);
      persistedAssets.add(new PersistedWork(assetWork, persistedAsset));
    }

    persistToSql(hibernateTemplate, articleWork, persistedArticle);
    for (PersistedWork persistedAsset : persistedAssets) {
      persistToSql(hibernateTemplate, persistedAsset.scholarlyWork, persistedAsset.result);
    }
  }

  private int persistToSql(HibernateTemplate hibernateTemplate, ScholarlyWork work, RepoCollectionMetadata persistedArticle) {
    return hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "INSERT INTO scholarlyWork " +
          "(doi, crepoKey, crepoUuid, scholarlyWorkType) VALUES " +
          "(:doi, :crepoKey, :crepoUuid, :scholarlyWorkType)");
      query.setParameter("doi", work.getDoi().getIdentifier());
      query.setParameter("scholarlyWorkType", work.getType());

      RepoVersion repoVersion = persistedArticle.getVersion();
      query.setParameter("crepoKey", repoVersion.getKey());
      query.setParameter("crepoUuid", repoVersion.getUuid().toString());

      return query.executeUpdate();
    });
  }

}
