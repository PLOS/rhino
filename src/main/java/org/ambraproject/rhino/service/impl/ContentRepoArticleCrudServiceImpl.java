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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteSource;

import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleItemIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleFileStorage;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.util.Archive;
import org.hibernate.Query;
import org.plos.crepo.exceptions.NotFoundException;
import org.plos.crepo.model.identity.RepoId;
import org.plos.crepo.model.identity.RepoVersion;
import org.plos.crepo.model.metadata.RepoObjectMetadata;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;

/**
 * Service implementing _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@SuppressWarnings("JpaQlInspection")
public class ContentRepoArticleCrudServiceImpl extends AbstractArticleCrudServiceImpl implements ArticleCrudService {
  @Autowired
  protected ContentRepoService contentRepoService;

  @Override
  public Document getManuscriptXml(ArticleIngestion ingestion) {
    return getManuscriptXml(getManuscriptMetadata(ingestion));
  }

  private Document getManuscriptXml(RepoObjectMetadata objectMetadata) {
    RepoVersion repoVersion = objectMetadata.getVersion();
    try (InputStream manuscriptInputStream = contentRepoService.getRepoObject(repoVersion)) {
      return parseXml(manuscriptInputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  public RepoObjectMetadata getManuscriptMetadata(ArticleIngestion ingestion) {
    Doi articleDoi = Doi.create(ingestion.getArticle().getDoi());
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(articleDoi, ingestion.getIngestionNumber());
    ArticleItemIdentifier articleItemId = ingestionId.getItemFor();
    ArticleFileIdentifier manuscriptId = ArticleFileIdentifier.create(articleItemId, "manuscript");
    RepoObjectMetadata objectMetadata = getArticleItemFileInternal(manuscriptId);
    return objectMetadata;
  }

  @Override
  public Archive repack(ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion ingestion = readIngestion(ingestionId);
    @SuppressWarnings("unchecked")
    List<ArticleFile> files = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM ArticleFile WHERE ingestion = :ingestion");
      query.setParameter("ingestion", ingestion);
      return (List<ArticleFile>) query.list();
    });
    

    Map<String, ByteSource> archiveMap = files.stream().collect(Collectors.toMap(
        ArticleFile::getIngestedFileName,
        (ArticleFile file) -> new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            RepoVersion repoVersion = RepoVersion.create(file.getBucketName(), file.getCrepoKey(), file.getCrepoUuid());
            return contentRepoService.getRepoObject(repoVersion);
          }
        }));

    return Archive.pack(extractFilenameStub(ingestionId.getDoiName()) + ".zip", archiveMap);
  }

  private static final Pattern FILENAME_STUB_PATTERN = Pattern.compile("(?:[^/]*/)*?([^/]*)/?");

  private static String extractFilenameStub(String name) {
    Matcher m = FILENAME_STUB_PATTERN.matcher(name);
    return m.matches() ? m.group(1) : name;
  }

  private RepoObjectMetadata getArticleItemFileInternal(ArticleFileIdentifier fileId) {
    ArticleItemIdentifier id = fileId.getItemIdentifier();
    ArticleItem work = getArticleItem(id);
    String fileType = fileId.getFileType();
    ArticleFile articleFile = work.getFile(fileType)
        .orElseThrow(() -> new RestClientException("Unrecognized type: " + fileType, HttpStatus.NOT_FOUND));
    try {
      RepoVersion repoVersion = RepoVersion.create(articleFile.getBucketName(), articleFile.getCrepoKey(), articleFile.getCrepoUuid());
      // throw new RuntimeException(contentRepoService.getRepoObjectMetadata(repoVersion).toString());
      return contentRepoService.getRepoObjectMetadata(repoVersion);
    } catch (NotFoundException e) {
      throw new RestClientException("Object not found: " + fileId + ". File info: " + articleFile,
          HttpStatus.NOT_FOUND);
    }
  }

  @Override
  public ArticleFileStorage getArticleItemFile(ArticleFileIdentifier fileId) {
    RepoObjectMetadata metadata = getArticleItemFileInternal(fileId);
    RepoVersion version = metadata.getVersion();
    RepoId id = version.getId();
    return ArticleFileStorage.builder()
      .setBucketName(id.getBucketName())
      .setContentType(metadata.getContentType())
      .setCrepoKey(id.getKey())
      .setDownloadName(metadata.getDownloadName())
      .setSize(metadata.getSize())
      .setTimestamp(metadata.getTimestamp())
      .setUuid(version.getUuid().toString())
      .build();
  }

  @Override
  public InputStream getRepoObjectInputStream(ArticleFileStorage metadata) {
    return contentRepoService.getRepoObject(RepoVersion.create(metadata.getBucketName(), metadata.getCrepoKey(), metadata.getUuid()));
  }
}
