/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.admin.service;

import org.ambraproject.admin.RestClientException;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Service implementing _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 * <p/>
 * This is proof-of-concept code; it isn't necessarily correct for representing articles in the database and
 * (especially) the file store.
 */
public class ArticleCrudServiceImpl extends AmbraService implements ArticleCrudService {

  private boolean articleExistsAt(String doi) {
    Long articleCount = (Long) hibernateTemplate.findByCriteria(DetachedCriteria
        .forClass(Article.class)
        .add(Restrictions.eq("doi", doi))
        .setProjection(Projections.rowCount())
    ).get(0);
    return articleCount > 0L;
  }

  private RestClientException reportDoiNotFound() {
    return new RestClientException("DOI does not belong to an article", HttpStatus.NOT_FOUND);
  }


  private void write(MultipartFile file, String doi) throws FileStoreException, IOException {
    byte[] inputData;
    InputStream input = null;
    try {
      input = file.getInputStream();
      inputData = IOUtils.toByteArray(input);
    } catch (IOException e) {
      throw new RestClientException("Could not read provided file", HttpStatus.BAD_REQUEST);
    } finally {
      IOUtils.closeQuietly(input);
    }

    OutputStream output = null;
    try {
      output = fileStoreService.getFileOutStream(doi, inputData.length);
      output.write(inputData);
    } finally {
      IOUtils.closeQuietly(output);
    }

  }


  @Override
  public void create(MultipartFile file, String doi) throws IOException, FileStoreException {
    if (articleExistsAt(doi)) {
      throw new RestClientException("Can't create article; DOI already exists", HttpStatus.METHOD_NOT_ALLOWED);
    }

    Article article = new Article();
    article.setDoi(doi);
    article.setDate(new Date());
    hibernateTemplate.save(article);

    write(file, doi);
  }

  @Override
  public byte[] read(String doi) throws FileStoreException {
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }

    // TODO Can an invalid request cause this to throw FileStoreException? If so, wrap in RestClientException.
    return fileStoreService.getFileByteArray(doi);
  }

  @Override
  public void update(MultipartFile file, String doi) throws IOException, FileStoreException {
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }
    write(file, doi);
  }

  @Override
  public void delete(String doi) throws FileStoreException {
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }

    Article article = (Article) hibernateTemplate.findByCriteria(DetachedCriteria
        .forClass(Article.class)
        .add(Restrictions.eq("doi", doi))
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)).get(0);
    hibernateTemplate.delete(article);

    fileStoreService.deleteFile(doi);
  }

}
