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

import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.StandAloneDoiCrudController;
import org.ambraproject.rhino.service.AssetCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

public class AssetMetadataController extends StandAloneDoiCrudController {

  private static final String ASSET_META_NAMESPACE = "/asset-meta/";
  private static final String ASSET_META_TEMPLATE = ASSET_META_NAMESPACE + "**";

  @Override
  protected String getNamespacePrefix() {
    return ASSET_META_NAMESPACE;
  }

  @Autowired
  private AssetCrudService assetCrudService;

  @RequestMapping(value = ASSET_META_TEMPLATE, method = RequestMethod.GET)
  public ResponseEntity<String> read(HttpServletRequest request,
                                     @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format) {
    DoiBasedIdentity id = parse(request);
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    String metadata = assetCrudService.readMetadata(id, mf);
    return new ResponseEntity<String>(metadata, HttpStatus.OK);
  }

}
