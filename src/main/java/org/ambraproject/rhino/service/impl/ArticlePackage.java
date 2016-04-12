package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.hibernate.SQLQuery;
import org.plos.crepo.model.RepoCollectionList;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class ArticlePackage {

  private final ScholarlyWork articleWork;
  private final ImmutableList<ScholarlyWork> assetWorks;

  ArticlePackage(ScholarlyWork articleWork, List<ScholarlyWork> assetWorks) {
    this.articleWork = Objects.requireNonNull(articleWork);
    this.assetWorks = ImmutableList.copyOf(assetWorks);
  }


  public void persist(HibernateTemplate hibernateTemplate, ContentRepoService contentRepoService) {
    RepoCollectionList persistedArticle = articleWork.persistToCrepo(contentRepoService);

    Map<DoiBasedIdentity, RepoCollectionList> persistedAssets = new LinkedHashMap<>();
    for (ScholarlyWork assetWork : assetWorks) {
      RepoCollectionList persistedAsset = assetWork.persistToCrepo(contentRepoService);
      persistedAssets.put(assetWork.getDoi(), persistedAsset);
    }

    persistToSql(hibernateTemplate, articleWork.getDoi(), persistedArticle);
    for (Map.Entry<DoiBasedIdentity, RepoCollectionList> entry : persistedAssets.entrySet()) {
      persistToSql(hibernateTemplate, entry.getKey(), entry.getValue());
    }
  }

  private int persistToSql(HibernateTemplate hibernateTemplate, DoiBasedIdentity doi, RepoCollectionMetadata persistedArticle) {
    return hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "INSERT INTO scholarlyWork " +
          "(doi, crepoKey, crepoUuid, scholarlyWorkType) VALUES " +
          "(:doi, :crepoKey, :crepoUuid, :scholarlyWorkType)");
      query.setParameter("doi", doi.getIdentifier());
      query.setParameter("scholarlyWorkType", "TODO"); // TODO

      RepoVersion repoVersion = persistedArticle.getVersion();
      query.setParameter("crepoKey", repoVersion.getKey());
      query.setParameter("crepoUuid", repoVersion.getUuid());

      return query.executeUpdate();
    });
  }

}
