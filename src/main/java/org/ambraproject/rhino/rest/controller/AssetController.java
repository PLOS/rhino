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

package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.service.AssetCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class AssetController extends RestController {

  @Autowired
  private AssetCrudService assetCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}/files", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("doi") String doi,
                   @PathVariable("number") int ingestionNumber)
      throws IOException {
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.resolve(doi), ingestionNumber);
    AssetIdentity id = null; // TODO: Implement AssetCrudService.readMetadata for ArticleIngestionIdentifier
    assetCrudService.readMetadata(id).respond(request, response, entityGson);
  }

}
