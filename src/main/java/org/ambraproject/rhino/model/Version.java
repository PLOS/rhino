/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2011 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambraproject.rhino.model;

/**
 * For storing the version of ambra
 *
 * @author Joe Osowski
 */
public class Version extends AmbraEntity {
  private String name;
  private int version;
  private boolean updateInProcess;

  public boolean getUpdateInProcess() {
    return updateInProcess;
  }

  public void setUpdateInProcess(boolean updateInProcess) {
    this.updateInProcess = updateInProcess;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Version)) return false;

    Version version1 = (Version) o;

    if (version != version1.version) return false;
    if (name != null ? !name.equals(version1.name) : version1.name != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + version;
    return result;
  }
}
