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
import org.ambraproject.filestore.FSIDMapper;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.http.HttpStatus;

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

  /**
   * Produce the file store ID for an article's base XML file.
   *
   * @param doi the DOI of an article
   * @return the FSID for the article's XML file
   * @throws RestClientException if the DOI can't be parsed and converted into an FSID
   */
  private static String findFsidForArticleXml(String doi) {
    String fsid = FSIDMapper.doiTofsid(doi, "XML");
    if (fsid.isEmpty()) {
      throw new RestClientException("DOI does not match expected format", HttpStatus.BAD_REQUEST);
    }
    return fsid;
  }


  /**
   * Read a client-provided stream into memory. Report it as a client error if the stream cannot be read. Closes the
   * stream.
   *
   * @param input an input stream from a RESTful request
   * @return a byte array of the input stream contents
   */
  private byte[] readClientInput(InputStream input) {
    try {
      return IOUtils.toByteArray(input);
    } catch (IOException e) {
      throw new RestClientException("Could not read provided file", HttpStatus.BAD_REQUEST, e);
    } finally {
      try {
        input.close();
      } catch (IOException e) {
        throw new RestClientException("Error closing file stream from client", HttpStatus.BAD_REQUEST, e);
      }
    }
  }

  /**
   * Write the base article XML to the file store. The DOI is used to generate the FSID. If something is already stored
   * for that DOI, it is overwritten; else, a new file is created.
   *
   * @param fileData the data to write, as raw bytes
   * @param doi      the article XML
   * @throws FileStoreException
   * @throws IOException
   */
  private void write(byte[] fileData, String doi) throws FileStoreException, IOException {
    String fsid = findFsidForArticleXml(doi);
    OutputStream output = null;
    try {
      output = fileStoreService.getFileOutStream(fsid, fileData.length);
      output.write(fileData);
    } finally {
      IOUtils.closeQuietly(output);
    }

  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void create(InputStream file, String doi) throws IOException, FileStoreException {
    if (articleExistsAt(doi)) {
      throw new RestClientException("Can't create article; DOI already exists", HttpStatus.METHOD_NOT_ALLOWED);
    }

    Article article = new Article();
    article.setDoi(doi);
    article.setDate(new Date());
    hibernateTemplate.save(article);

    write(readClientInput(file), doi);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public byte[] read(String doi) throws FileStoreException {
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }
    String fsid = findFsidForArticleXml(doi);

    // TODO Can an invalid request cause this to throw FileStoreException? If so, wrap in RestClientException.
    return fileStoreService.getFileByteArray(fsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void update(InputStream file, String doi) throws IOException, FileStoreException {
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }
    write(readClientInput(file), doi);
  }

  /**
   * {@inheritDoc}
   */
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
