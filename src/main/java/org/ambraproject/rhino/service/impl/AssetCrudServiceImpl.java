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

import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.plos.crepo.exceptions.NotFoundException;
import org.plos.crepo.model.metadata.RepoObjectMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class AssetCrudServiceImpl extends AmbraService implements AssetCrudService {

  @Autowired
  private ArticleCrudService articleCrudService;

  @Override
  public RepoObjectMetadata getArticleItemFile(ArticleFileIdentifier fileId) {
    ArticleItem work = articleCrudService.getArticleItem(fileId.getItemIdentifier());
    String fileType = fileId.getFileType();
    ArticleFile articleFile = work.getFile(fileType)
        .orElseThrow(() -> new RestClientException("Unrecognized type: " + fileType, HttpStatus.NOT_FOUND));
    try {
      return contentRepoService.getRepoObjectMetadata(articleFile.getCrepoVersion());
    } catch (NotFoundException e) {
      throw new RestClientException("Object not found: " + fileId + ". File info: " + articleFile,
          HttpStatus.NOT_FOUND);
    }
  }

}
