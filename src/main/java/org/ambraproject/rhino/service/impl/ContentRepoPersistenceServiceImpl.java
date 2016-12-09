package org.ambraproject.rhino.service.impl;

import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ingest.ArticleFileInput;
import org.ambraproject.rhino.model.ingest.ArticleItemInput;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.plos.crepo.model.identity.RepoId;
import org.plos.crepo.model.identity.RepoVersion;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class ContentRepoPersistenceServiceImpl implements ContentRepoPersistenceService {

  @Autowired
  private ContentRepoService contentRepoService;

  @Override
  public ArticleItem createItem(ArticleItemInput itemInput, ArticleIngestion ingestion) {
    ArticleItem item = new ArticleItem();
    item.setIngestion(ingestion);
    item.setDoi(itemInput.getDoi().getName());
    item.setItemType(itemInput.getType());

    Collection<ArticleFile> files = new ArrayList<>(itemInput.getFiles().entrySet().size());
    for (Map.Entry<String, ArticleFileInput> entry : itemInput.getFiles().entrySet()) {
      ArticleFileInput fileInput = entry.getValue();
      RepoVersion repoVersion = contentRepoService.autoCreateRepoObject(fileInput.getObject())
          .getVersion();

      ArticleFile file = new ArticleFile();
      file.setIngestion(ingestion);
      file.setItem(item);
      file.setFileType(entry.getKey());

      RepoId repoId = repoVersion.getId();
      file.setBucketName(repoId.getBucketName());
      file.setCrepoKey(repoId.getKey());
      file.setCrepoUuid(repoVersion.getUuid().toString());

      file.setFileSize(contentRepoService.getRepoObjectMetadata(repoVersion).getSize());
      file.setIngestedFileName(fileInput.getFilename());

      files.add(file);
    }
    item.setFiles(files);

    return item;
  }

  // TODO: 12/9/16 merge some of the shared logic between these two methods
  @Override
  public Collection<ArticleFile> persistAncillaryFiles(ArticlePackage articlePackage,
                                                       ArticleIngestion ingestion) {
    Collection<ArticleFile> files = new ArrayList<>();
    for (ArticleFileInput ancillaryFile : articlePackage.getAncillaryFiles()) {
      RepoVersion repoVersion = contentRepoService.autoCreateRepoObject(ancillaryFile.getObject())
          .getVersion();

      ArticleFile file = new ArticleFile();
      file.setIngestion(ingestion);
      file.setFileSize(contentRepoService.getRepoObjectMetadata(repoVersion).getSize());
      file.setIngestedFileName(ancillaryFile.getFilename());

      RepoId repoId = repoVersion.getId();
      file.setBucketName(repoId.getBucketName());
      file.setCrepoKey(repoId.getKey());
      file.setCrepoUuid(repoVersion.getUuid().toString());

      files.add(file);
    }
    return files;
  }
}
