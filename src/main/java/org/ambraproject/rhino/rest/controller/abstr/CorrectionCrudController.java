/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.rest.controller.abstr;

import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class CorrectionCrudController extends DoiBasedCrudController {

  private static final String CORRECTION_META_NAMESPACE = "/corrections/";
  private static final String CORRECTION_META_TEMPLATE = CORRECTION_META_NAMESPACE + "**";

  @Override
  protected String getNamespacePrefix() {
    return CORRECTION_META_NAMESPACE;
  }

  @Autowired
  private AnnotationCrudService annotationCrudService;

  @RequestMapping(value = CORRECTION_META_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DoiBasedIdentity id = parse(request);
    MetadataFormat mf = MetadataFormat.getFromRequest(request);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    annotationCrudService.readCorrection(receiver, id, mf);
  }
}
