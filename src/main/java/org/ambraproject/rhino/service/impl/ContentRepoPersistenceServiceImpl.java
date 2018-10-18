/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.service.impl;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ingest.ArticleFileInput;
import org.ambraproject.rhino.model.ingest.ArticleItemInput;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.service.ContentRepoPersistenceService;
import org.plos.crepo.model.identity.RepoId;
import org.plos.crepo.model.identity.RepoVersion;
import org.plos.crepo.model.input.RepoObjectInput;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class ContentRepoPersistenceServiceImpl implements ContentRepoPersistenceService {

  @Autowired
  private ContentRepoService contentRepoService;

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  private String bucketName() {
    RuntimeConfiguration.ContentRepoEndpoint corpusStorage = runtimeConfiguration.getCorpusStorage();
    return corpusStorage.getBucket();
  }

  private RepoObjectInput makeRepoObjectInput(ArticleFileInput fileInput) {
    return RepoObjectInput.builder(bucketName(), fileInput.getManifestFile().getCrepoKey())
        .setContentAccessor(() -> fileInput.getArchive().openFile(fileInput.getFilename()))
        .setContentType(fileInput.getContentType())
        .setDownloadName(fileInput.getDownloadName())
        .build();
  }

  @Override
  public ArticleItem createItem(ArticleItemInput itemInput, ArticleIngestion ingestion) {
    ArticleItem item = new ArticleItem();
    item.setIngestion(ingestion);
    item.setDoi(itemInput.getDoi().getName());
    item.setItemType(itemInput.getType());

    Collection<ArticleFile> files = new ArrayList<>(itemInput.getFiles().entrySet().size());
    for (Map.Entry<String, ArticleFileInput> entry : itemInput.getFiles().entrySet()) {
      ArticleFileInput fileInput = entry.getValue();
      RepoVersion repoVersion = contentRepoService.autoCreateRepoObject(makeRepoObjectInput(fileInput)).getVersion();

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
      RepoVersion repoVersion = contentRepoService.autoCreateRepoObject(makeRepoObjectInput(ancillaryFile)).getVersion();

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
