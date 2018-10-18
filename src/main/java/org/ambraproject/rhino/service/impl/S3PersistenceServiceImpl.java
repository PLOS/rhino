/*
 * Copyright (c) 2018 Public Library of Science
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

import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ingest.ArticleFileInput;
import org.ambraproject.rhino.model.ingest.ArticleItemInput;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.service.ContentPersistenceService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

public class S3PersistenceServiceImpl implements ContentPersistenceService {
  private String bucketName = "MY_BUCKET";

  @Autowired
  private AmazonS3 amazonS3;

  // public S3PersistenceServiceImpl(String bucketName) {
  //   this.bucketName = bucketName;
  // }
  
  private PutObjectResult uploadFile(ArticleFileInput fileInput, String key) {
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentType(fileInput.getContentType());
    metadata.setContentDisposition(String.format("attachment; filename=%s", fileInput.getDownloadName()));
    InputStream input = fileInput.getArchive().openFile(fileInput.getFilename());
    PutObjectRequest request = new PutObjectRequest(bucketName, key, input, metadata);
    return amazonS3.putObject(request);
  }

  public static String getS3Key(ArticleIngestion ingestion, ArticleFileInput fileInput) {
    return String.format("%s/v%d/%s",
                         ingestion.getArticle().getDoi(),
                         ingestion.getIngestionNumber(),
                         fileInput.getManifestFile().getCrepoKey());
  }

  @Override
  public ArticleItem createItem(ArticleItemInput itemInput, ArticleIngestion ingestion) {
    ArticleItem item = new ArticleItem();
    item.setIngestion(ingestion);
    item.setDoi(itemInput.getDoi().getName());
    item.setItemType(itemInput.getType());
    Collection<ArticleFile> files = new ArrayList<>();
    for (Map.Entry<String, ArticleFileInput> entry : itemInput.getFiles().entrySet()) {
      String key = getS3Key(ingestion, entry.getValue());
      PutObjectResult result = uploadFile(entry.getValue(), key);
      ArticleFile file = new ArticleFile();
      file.setIngestion(ingestion);
      file.setItem(item);
      file.setFileType(entry.getKey());

      file.setBucketName(bucketName);
      file.setCrepoKey(key);
      file.setCrepoUuid(UUID.randomUUID().toString()); // TODO: Drop this column
      file.setFileSize(result.getMetadata().getContentLength());
      file.setIngestedFileName(entry.getValue().getFilename());
      files.add(file);
    }
    item.setFiles(files);
    return item;
  }

  @Override
  public Collection<ArticleFile> persistAncillaryFiles(ArticlePackage articlePackage,
                                                       ArticleIngestion ingestion) {
    Collection<ArticleFile> files = new ArrayList<>();
    for (ArticleFileInput ancillaryFile : articlePackage.getAncillaryFiles()) {
      String key = getS3Key(ingestion, ancillaryFile);
      PutObjectResult result = uploadFile(ancillaryFile, key);
      ArticleFile file = new ArticleFile();
      file.setIngestion(ingestion);

      file.setBucketName(bucketName);
      file.setCrepoKey(key);
      file.setCrepoUuid(UUID.randomUUID().toString()); // TODO: Drop this column
      file.setFileSize(result.getMetadata().getContentLength());
      file.setIngestedFileName(ancillaryFile.getFilename());
      files.add(file);
    }
    return files;
  }
}
