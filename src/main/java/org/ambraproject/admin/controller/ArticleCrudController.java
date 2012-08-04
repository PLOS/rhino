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

package org.ambraproject.admin.controller;

import org.ambraproject.admin.RestClientException;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 * <p/>
 * This is proof-of-concept code; it isn't necessarily correct for representing articles in the database and
 * (especially) the file store.
 */
@Controller
public class ArticleCrudController extends RestController {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudController.class);

  private static final String DOI_SCHEME_VALUE = "info:doi/";

  private static final String DOI_PREFIX = "doiPrefix";
  private static final String DOI_ID = "doiId";
  private static final String DOI_TEMPLATE = "article/{" + DOI_PREFIX + "}/{" + DOI_ID + "}";
  private static final String FILE_ARG = "file";

  private static String buildDoi(String prefix, String id) {
    return DOI_SCHEME_VALUE + prefix + '/' + id;
  }

  private RestClientException reportDoiNotFound() {
    return new RestClientException("DOI does not belong to an article", HttpStatus.NOT_FOUND);
  }


  private ResponseEntity<?> write(MultipartFile file, String doi) throws FileStoreException, IOException {
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
      output = getFileStoreService().getFileOutStream(doi, inputData.length);
      output.write(inputData);
    } finally {
      IOUtils.closeQuietly(output);
    }

    return reportOk();
  }

  private boolean articleExistsAt(String doi) {
    Long articleCount = (Long) getHibernateTemplate().findByCriteria(DetachedCriteria
        .forClass(Article.class)
        .add(Restrictions.eq("doi", doi))
        .setProjection(Projections.rowCount())
    ).get(0);
    return articleCount > 0L;
  }

  @RequestMapping(value = DOI_TEMPLATE, method = RequestMethod.POST)
  public ResponseEntity<?> create(@RequestParam(FILE_ARG) MultipartFile file,
                                  @PathVariable(DOI_PREFIX) String doiPrefix, @PathVariable(DOI_ID) String doiId)
      throws IOException, FileStoreException {
    final String doi = buildDoi(doiPrefix, doiId);
    if (articleExistsAt(doi)) {
      throw new RestClientException("Can't create article; DOI already exists", HttpStatus.METHOD_NOT_ALLOWED);
    }

    Article article = new Article();
    article.setDoi(doi);
    article.setDate(new Date());
    getHibernateTemplate().save(article);

    return write(file, doi);
  }

  @RequestMapping(value = DOI_TEMPLATE, method = RequestMethod.GET)
  public ResponseEntity<?> read(@PathVariable(DOI_PREFIX) String doiPrefix, @PathVariable(DOI_ID) String doiId)
      throws FileStoreException {
    final String doi = buildDoi(doiPrefix, doiId);
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }

    // TODO Can an invalid request cause this to throw FileStoreException?
    byte[] fileData = getFileStoreService().getFileByteArray(doi);

    return new ResponseEntity<byte[]>(fileData, HttpStatus.OK);
  }

  @RequestMapping(value = DOI_TEMPLATE, method = RequestMethod.PUT)
  public ResponseEntity<?> update(@RequestParam("file") MultipartFile file,
                                  @PathVariable(DOI_PREFIX) String doiPrefix, @PathVariable(DOI_ID) String doiId)
      throws IOException, FileStoreException {
    final String doi = buildDoi(doiPrefix, doiId);
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }

    return write(file, doi);
  }

  @RequestMapping(value = DOI_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(@PathVariable(DOI_PREFIX) String doiPrefix, @PathVariable(DOI_ID) String doiId) throws FileStoreException {
    final String doi = buildDoi(doiPrefix, doiId);
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }

    HibernateTemplate hibernateTemplate = getHibernateTemplate();
    Article article = (Article) hibernateTemplate.findByCriteria(DetachedCriteria
        .forClass(Article.class)
        .add(Restrictions.eq("doi", doi))
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)).get(0);
    hibernateTemplate.delete(article);

    getFileStoreService().deleteFile(doi);
    return reportOk();
  }

}
