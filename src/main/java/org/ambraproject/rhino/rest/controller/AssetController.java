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

import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.AssetCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class AssetController extends DoiBasedCrudController {

  private static final String ASSET_META_NAMESPACE = "/assets/";
  private static final String ASSET_META_TEMPLATE = ASSET_META_NAMESPACE + "**";

  @Override
  protected String getNamespacePrefix() {
    return ASSET_META_NAMESPACE;
  }

  @Override
  protected AssetIdentity parse(HttpServletRequest request) {
    return AssetIdentity.create(getIdentifier(request));
  }

  @Autowired
  private AssetCrudService assetCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = ASSET_META_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    AssetIdentity id = parse(request);
    assetCrudService.readMetadata(id).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = ASSET_META_TEMPLATE, params = {"figure"}, method = RequestMethod.GET)
  public void readAsFigure(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    AssetIdentity id = parse(request);
    assetCrudService.readFigureMetadata(id).respond(request, response, entityGson);
  }

}
