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

import org.ambraproject.admin.service.VolumeCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

public class VolumeCrudController extends DoiBasedCrudController {

  private static final String VOLUME_NAMESPACE = "/volume/";
  private static final String VOLUME_TEMPLATE = VOLUME_NAMESPACE + "**";

  private static final String DISPLAY_PARAM = "display";
  private static final String JOURNAL_PARAM = "journal";

  @Override
  protected String getNamespacePrefix() {
    return VOLUME_NAMESPACE;
  }

  @Autowired
  private VolumeCrudService volumeCrudService;


  @RequestMapping(value = VOLUME_TEMPLATE, method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request,
                                  @RequestParam(DISPLAY_PARAM) String displayName,
                                  @RequestParam(JOURNAL_PARAM) String journalKey) {
    DoiBasedIdentity id = parse(request);
    volumeCrudService.create(id, displayName, journalKey);
    return reportCreated();
  }

}
