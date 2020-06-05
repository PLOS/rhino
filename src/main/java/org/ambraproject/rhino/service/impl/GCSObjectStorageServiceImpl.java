/*
 * Copyright (c) 2020 Public Library of Science
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
import org.ambraproject.rhino.service.ObjectStorageService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

public class GCSObjectStorageServiceImpl implements ObjectStorageService {
  @Autowired
  private Storage storage;

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  private String bucketName() {
    return runtimeConfiguration.getCorpusBucket();
  }
  
  private Blob uploadFile(ArticleFileInput fileInput, String key) {
    BlobId blobId = BlobId.of(bucketName(), key);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
      .setContentType(fileInput.getContentType())
      .setContentDisposition(String.format("attachment; filename=%s", fileInput.getDownloadName()))
      .build();
    InputStream input = fileInput.getArchive().openFile(fileInput.getFilename());
    return storage.create(blobInfo, input);
  }

  public static String getObjectKey(ArticleIngestion ingestion, ArticleFileInput fileInput) {
    return String.format("%s/%d/%s",
                         ingestion.getArticle().getDoi(),
                         ingestion.getIngestionNumber(),
                         fileInput.getFilename());
  }

  @Override
  public ArticleItem storeItem(ArticleItemInput itemInput, ArticleIngestion ingestion) {
    ArticleItem item = new ArticleItem();
    item.setIngestion(ingestion);
    item.setDoi(itemInput.getDoi().getName());
    item.setItemType(itemInput.getType());
    Collection<ArticleFile> files = new ArrayList<>();
    for (Map.Entry<String, ArticleFileInput> entry : itemInput.getFiles().entrySet()) {
      String key = getObjectKey(ingestion, entry.getValue());
      Blob result = uploadFile(entry.getValue(), key);
      ArticleFile file = new ArticleFile();
      file.setIngestion(ingestion);
      file.setItem(item);
      file.setFileType(entry.getKey());

      file.setFileSize(result.getSize());
      file.setIngestedFileName(entry.getValue().getFilename());
      files.add(file);
    }
    item.setFiles(files);
    return item;
  }

  @Override
  public Collection<ArticleFile> storeAncillaryFiles(ArticlePackage articlePackage,
                                                       ArticleIngestion ingestion) {
    Collection<ArticleFile> files = new ArrayList<>();
    for (ArticleFileInput ancillaryFile : articlePackage.getAncillaryFiles()) {
      String key = getObjectKey(ingestion, ancillaryFile);
      Blob result = uploadFile(ancillaryFile, key);
      ArticleFile file = new ArticleFile();
      file.setIngestion(ingestion);

      file.setFileSize(result.getSize());
      file.setIngestedFileName(ancillaryFile.getFilename());
      files.add(file);
    }
    return files;
  }
}
