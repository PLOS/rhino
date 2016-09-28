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
      throw new RestClientException("Object not found: " + fileId, HttpStatus.NOT_FOUND);
    }
  }

}
