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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import com.google.api.client.util.Base64;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.common.io.ByteStreams;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ingest.ArticleFileInput;
import org.ambraproject.rhino.model.ingest.ArticleItemInput;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.service.ObjectStorageService;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class GCSObjectStorageServiceImpl implements ObjectStorageService {
  @Autowired
  private Storage storage;

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  private String bucketName() {
    return runtimeConfiguration.getCorpusBucket();
  }
  
  private BlobInfo uploadFile(ArticleFileInput fileInput, String key) {
    BlobId blobId = BlobId.of(bucketName(), key);
    String md5;
    try (InputStream inputStream = fileInput.getArchive().openFile(fileInput.getFilename())) {
      md5 = Base64.encodeBase64String(DigestUtils.md5(inputStream));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    try (InputStream inputStream = fileInput.getArchive().openFile(fileInput.getFilename())) {
      BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(fileInput.getContentType())
        .setContentDisposition(String.format("attachment; filename=%s", fileInput.getDownloadName()))
        .setMd5(md5)
        .build();
      try (WriteChannel writer = storage.writer(blobInfo, Storage.BlobWriteOption.md5Match())) {
        ByteStreams.copy(inputStream, Channels.newOutputStream(writer));
      }
    } catch (StorageException ex) {
      if (!(400 == ex.getCode() && "invalid".equals(ex.getReason()))) {
        throw ex;
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
    return storage.get(blobId);
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
      BlobInfo result = uploadFile(entry.getValue(), key);
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
      BlobInfo result = uploadFile(ancillaryFile, key);
      ArticleFile file = new ArticleFile();
      file.setIngestion(ingestion);

      file.setFileSize(result.getSize());
      file.setIngestedFileName(ancillaryFile.getFilename());
      files.add(file);
    }
    return files;
  }
}
