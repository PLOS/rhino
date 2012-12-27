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

package org.ambraproject.admin.rest.controller.abstr;

import com.google.common.io.Closeables;
import org.ambraproject.admin.identity.AssetIdentity;
import org.ambraproject.admin.identity.DoiBasedIdentity;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on entities identified by a {@link
 * org.ambraproject.admin.identity.DoiBasedIdentity}.
 */
public abstract class DoiBasedCrudController<I extends DoiBasedIdentity> extends RestController {

  protected static final String METADATA_FORMAT_PARAM = "format";

  /**
   * Return the URL prefix that describes the RESTful namespace that this controller handles. It should include a
   * leading and trailing slash. Typically this is a constant.
   *
   * @return the constant URL prefix
   */
  protected abstract String getNamespacePrefix();

  protected String getIdentifier(HttpServletRequest request) {
    return getFullPathVariable(request, getNamespacePrefix());
  }

  protected abstract I parse(HttpServletRequest request);

  protected ResponseEntity<byte[]> respondWithStream(InputStream stream, AssetIdentity identity) throws IOException {
    byte[] data;
    boolean threw = true;
    try {
      data = IOUtils.toByteArray(stream); // TODO Avoid dumping into memory?
      threw = false;
    } finally {
      Closeables.close(stream, threw);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(identity.getContentType());

    return new ResponseEntity<byte[]>(data, headers, HttpStatus.OK);
  }

}
