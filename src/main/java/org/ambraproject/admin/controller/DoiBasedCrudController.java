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

import javax.servlet.http.HttpServletRequest;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on entities identified by a {@link
 * DoiBasedIdentity}.
 */
public abstract class DoiBasedCrudController extends RestController {

  /**
   * The request parameter name for uploading a single file. Part of this application's public REST API.
   */
  protected static final String FILE_ARG = "file";

  /**
   * Return the URL prefix that describes the RESTful namespace that this controller handles. It should include a
   * leading and trailing slash. Typically this is a constant.
   *
   * @return the constant URL prefix
   */
  protected abstract String getNamespacePrefix();

  protected DoiBasedIdentity parse(HttpServletRequest request) {
    return DoiBasedIdentity.parse(request.getRequestURI(), getNamespacePrefix(), hasAssociatedFile());
  }

  /**
   * @return whether entities handled by this controller have a file in the file store associated with them
   * @see FileStoreController
   */
  protected boolean hasAssociatedFile() {
    return false;
  }

}
