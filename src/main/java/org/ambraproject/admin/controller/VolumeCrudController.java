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

import com.google.common.io.Closeables;
import org.ambraproject.admin.service.VolumeCrudService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

@Controller
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


  @RequestMapping(value = VOLUME_TEMPLATE, method = RequestMethod.PUT)
  public ResponseEntity<?> create(HttpServletRequest request,
                                  @RequestParam(DISPLAY_PARAM) String displayName,
                                  @RequestParam(JOURNAL_PARAM) String journalKey) {
    DoiBasedIdentity id = parse(request);
    volumeCrudService.create(id, displayName, journalKey);
    return reportCreated();
  }

  /*
   * Always assume the user wants the metadata as JSON.
   *
   * TODO: Add way to specify metadata format to API and make this consistent with it.
   */
  @RequestMapping(value = VOLUME_TEMPLATE, method = RequestMethod.GET)
  public ResponseEntity<?> read(HttpServletRequest request) throws IOException {
    DoiBasedIdentity id = parse(request);
    InputStream stream = null;
    try {
      stream = volumeCrudService.readJson(id);
      byte[] data = IOUtils.toByteArray(stream);
      return new ResponseEntity<String>(new String(data), HttpStatus.OK);
    } finally {
      Closeables.closeQuietly(stream);
    }
  }

}
