/*
 * Copyright (c) 2006-2014 by Public Library of Science
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

package org.ambraproject.rhino.service.impl;

import org.ambraproject.models.UserProfile;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.UserCrudService;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.user.AuthIdList;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class UserCrudServiceImpl extends AmbraService implements UserCrudService {

  /**
   * {@inheritDoc}
   */
  @Override
  public Transceiver listUsers() throws IOException {
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {

        List<String> authIds = (List<String>) hibernateTemplate.findByCriteria(
            DetachedCriteria
                .forClass(UserProfile.class)
                .setProjection(Projections.projectionList().add(Projections.property("authId")))
        );

        return new AuthIdList(authIds);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Transceiver read(final String authId) throws IOException {

    return new EntityTransceiver<UserProfile>() {

      @Override
      protected UserProfile fetchEntity() {
        UserProfile userProfile = (UserProfile) DataAccessUtils.uniqueResult(
            hibernateTemplate.findByCriteria(
                DetachedCriteria
                    .forClass(UserProfile.class)
                    .add(Restrictions.eq("authId", authId))
            )
        );

        if (userProfile == null) {
          throw new RestClientException("UserProfile not found at authId=" + authId, HttpStatus.NOT_FOUND);
        }

        return userProfile;
      }

      @Override
      protected Object getView(UserProfile userProfile) {
        return userProfile;
      }
    };
  }
}
