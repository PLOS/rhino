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

import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleFileStorage;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.springframework.beans.factory.annotation.Autowired;

public class GCSArticleCrudServiceImpl extends AbstractArticleCrudServiceImpl implements ArticleCrudService {
  @Autowired
  protected Storage storage;

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  String bucketName() {
    return runtimeConfiguration.getCorpusBucket();
  }
  
  public static String getObjectKey(ArticleFile file) {
    ArticleIngestion ingestion = file.getIngestion();
    return String.format("%s/%d/%s",
                         ingestion.getArticle().getDoi(),
                         ingestion.getIngestionNumber(),
                         file.getIngestedFileName());
  }

  long PRESIGNED_URL_EXPIRY = 1; // HOUR

  
  @Override
  public URL getUrl(ArticleFile file) {
    String objectKey = getObjectKey(file);
    BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName(), objectKey)).build();
    return storage.signUrl(blobInfo, PRESIGNED_URL_EXPIRY, TimeUnit.HOURS, Storage.SignUrlOption.withV4Signature());
  }

  @Override
  public ArticleFileStorage getArticleItemFile(ArticleFileIdentifier fileId) {
    ArticleFile articleFile = getArticleFile(fileId);
    String key = getObjectKey(articleFile);
    Blob blob = storage.get(BlobId.of(bucketName(), key));

    return ArticleFileStorage.builder()
      .setContentType(Optional.ofNullable(blob.getContentType()))
      .setSize(blob.getSize())
      .setTimestamp(new Timestamp(blob.getUpdateTime()))
      .build();
  }

  @Override
  public InputStream getInputStream(ArticleFileStorage metadata) {
    String key = metadata.getObjectKey().get();
    Blob blob = storage.get(BlobId.of(bucketName(), key));
    return Channels.newInputStream(blob.reader());
  }

  public InputStream getInputStream(ArticleFile metadata) {
    String key = getObjectKey(metadata);
    Blob blob = storage.get(BlobId.of(bucketName(), key));
    return Channels.newInputStream(blob.reader());
  }
}

