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

package org.ambraproject.admin.service;

import org.ambraproject.admin.controller.DoiBasedIdentity;
import org.ambraproject.filestore.FileStoreException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Super-interface to CRUD services within the article namespace.
 * <p/>
 * Methods are included here only if they share a common signature among services. See the extending interfaces for
 * documentation on the behavior of each method.
 */
public abstract interface DoiBasedCrudService {

  public abstract InputStream read(DoiBasedIdentity id) throws FileStoreException;

  public abstract void update(InputStream file, DoiBasedIdentity id) throws FileStoreException, IOException;

  public abstract void delete(DoiBasedIdentity id) throws FileStoreException;

}
