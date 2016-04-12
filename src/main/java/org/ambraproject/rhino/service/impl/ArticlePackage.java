package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.hibernate.SQLQuery;
import org.plos.crepo.model.RepoCollectionList;
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

  public DoiBasedIdentity getDoi() {
    return articleWork.getDoi();
  }

  public List<DoiBasedIdentity> getAssetDois() {
    return Lists.transform(assetWorks, ScholarlyWork::getDoi);
  }


  private static class PersistedWork {
    private final ScholarlyWork scholarlyWork;
    private final RepoCollectionList result;

    private PersistedWork(ScholarlyWork scholarlyWork, RepoCollectionList result) {
      this.scholarlyWork = Objects.requireNonNull(scholarlyWork);
      this.result = Objects.requireNonNull(result);
    }
  }

  public RepoCollectionList persist(HibernateTemplate hibernateTemplate, ContentRepoService contentRepoService) {
    RepoCollectionList persistedArticle = articleWork.persistToCrepo(contentRepoService);

    List<PersistedWork> persistedAssets = new ArrayList<>();
    for (ScholarlyWork assetWork : assetWorks) {
      RepoCollectionList persistedAsset = assetWork.persistToCrepo(contentRepoService);
      persistedAssets.add(new PersistedWork(assetWork, persistedAsset));
    }

    persistToSql(hibernateTemplate, new PersistedWork(articleWork, persistedArticle));
    for (PersistedWork persistedAsset : persistedAssets) {
      persistToSql(hibernateTemplate, persistedAsset);
      persistRelation(hibernateTemplate, persistedArticle, persistedAsset);
    }

    return persistedArticle;
  }

  private int persistToSql(HibernateTemplate hibernateTemplate, PersistedWork persistedWork) {
    return hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "INSERT INTO scholarlyWork " +
          "(doi, crepoKey, crepoUuid, scholarlyWorkType) VALUES " +
          "(:doi, :crepoKey, :crepoUuid, :scholarlyWorkType)");
      query.setParameter("doi", persistedWork.scholarlyWork.getDoi().getIdentifier());
      query.setParameter("scholarlyWorkType", persistedWork.scholarlyWork.getType());

      RepoVersion repoVersion = persistedWork.result.getVersion();
      query.setParameter("crepoKey", repoVersion.getKey());
      query.setParameter("crepoUuid", repoVersion.getUuid().toString());

      return query.executeUpdate();
    });
  }

  private void persistRelation(HibernateTemplate hibernateTemplate, RepoCollectionList article, PersistedWork asset) {
    hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "INSERT INTO scholarlyWorkRelation (originWorkId, targetWorkId, relationType) " +
          "VALUES (" +
          "  (SELECT scholarlyWorkId FROM scholarlyWork WHERE crepoKey=:originKey AND crepoUuid=:originUuid), " +
          "  (SELECT scholarlyWorkId FROM scholarlyWork WHERE crepoKey=:targetKey AND crepoUuid=:targetUuid), " +
          "  :relationType)");
      query.setParameter("relationType", "assetOf");

      RepoVersion articleVersion = article.getVersion();
      query.setParameter("originKey", articleVersion.getKey());
      query.setParameter("originUuid", articleVersion.getUuid().toString());

      RepoVersion assetVersion = asset.result.getVersion();
      query.setParameter("targetKey", assetVersion.getKey());
      query.setParameter("targetUuid", assetVersion.getUuid().toString());

      return query.executeUpdate();
    });
  }

}
