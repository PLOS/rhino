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

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Optional;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleFileStorage;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.springframework.beans.factory.annotation.Autowired;

public class S3ArticleCrudServiceImpl extends AbstractArticleCrudServiceImpl implements ArticleCrudService {
  @Autowired
  protected AmazonS3 amazonS3;

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  String bucketName() {
    return runtimeConfiguration.getCorpusBucket();
  }
  
  public static String getS3Key(ArticleFile file) {
    ArticleIngestion ingestion = file.getIngestion();
    return String.format("%s/%d/%s",
                         ingestion.getArticle().getDoi(),
                         ingestion.getIngestionNumber(),
                         file.getIngestedFileName());
  }

  @Override
  public ArticleFileStorage getArticleItemFile(ArticleFileIdentifier fileId) {
    ArticleFile articleFile = getArticleFile(fileId);
    String key = getS3Key(articleFile);
    GetObjectRequest request = new GetObjectRequest(bucketName(), key);
    S3Object object = amazonS3.getObject(request);
    ObjectMetadata metadata = object.getObjectMetadata();

    return ArticleFileStorage.builder()
      .setContentType(Optional.ofNullable(metadata.getContentType()))
      .setSize(metadata.getContentLength())
      .setTimestamp(new Timestamp(metadata.getLastModified().getTime()))
      .build();
  }

  @Override
  public InputStream getInputStream(ArticleFileStorage metadata) {
    String key = metadata.getS3Key().get();
    GetObjectRequest request = new GetObjectRequest(bucketName(), key);
    return amazonS3.getObject(request).getObjectContent();
  }

  public InputStream getInputStream(ArticleFile metadata) {
    String key = getS3Key(metadata);
    GetObjectRequest request = new GetObjectRequest(bucketName(), key);
    return amazonS3.getObject(request).getObjectContent();
  }
}

