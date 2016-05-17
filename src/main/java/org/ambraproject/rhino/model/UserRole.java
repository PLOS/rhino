/*
 * Copyright (c) 2006-2014 by Public Library of Science
 *
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambraproject.rhino.model;

import org.ambraproject.rhino.config.PersistenceAdapter;
import org.apache.commons.lang.ArrayUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class representing a role for a user
 *
 * @author Alex Kudlick 2/9/12
 */
public class UserRole extends AmbraEntity {

  public static enum Permission {
    ACCESS_ADMIN,
    INGEST_ARTICLE,
    MANAGE_FLAGS,
    MANAGE_ANNOTATIONS,
    MANAGE_USERS,
    MANAGE_ROLES,
    MANAGE_JOURNALS,
    MANAGE_SEARCH,
    MANAGE_CACHES,
    MANAGE_ARTICLE_LISTS,
    CROSS_PUB_ARTICLES,
    DELETE_ARTICLES,
    VIEW_UNPUBBED_ARTICLES,
    MANAGE_CORRECTIONS,
    RESEND_EMAIL_ALERTS,
    BETA_FEATURES,
    MANAGE_FEATURED_ARTICLES,
    TEST_THESAURUS
  }

  public static final PersistenceAdapter<Permission, String> PERMISSION_ADAPTER = PersistenceAdapter.byEnumName(Permission.class);

  private String roleName;
  private Set<Permission> permissions;

  public UserRole() {
    super();
  }

  public UserRole(String roleName, Permission... permissions) {
    this();
    this.roleName = roleName;
    if (!ArrayUtils.isEmpty(permissions)) {
      this.permissions = new HashSet<Permission>();
      Collections.addAll(this.permissions, permissions);
    }
  }

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  public Set<Permission> getPermissions() {
    return permissions;
  }

  public void setPermissions(Set<Permission> permissions) {
    this.permissions = permissions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UserRole)) return false;

    UserRole role = (UserRole) o;

    if (roleName != null ? !roleName.equals(role.roleName) : role.roleName != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return roleName != null ? roleName.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "UserRole{" +
        "roleName='" + roleName + '\'' +
        '}';
  }
}
