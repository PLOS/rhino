package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import org.ambraproject.configuration.ConfigurationStore;
import org.ambraproject.models.UserLogin;
//import org.ambraproject.models.UserProfile;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.UserCrudService;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.user.AuthIdList;
import org.apache.commons.configuration.Configuration;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class UserCrudServiceImpl extends AmbraService implements UserCrudService {

  @Autowired
  private Configuration ambraConfiguration;

  private boolean advancedLogging = false;

  @PostConstruct
  public void init() {
    Object val = ambraConfiguration.getProperty(ConfigurationStore.ADVANCED_USAGE_LOGGING);
    if (val != null && val.equals("true")) {
      this.advancedLogging = true;
    }
  }


  /**
   * {@inheritDoc}
   */
//  @Override
//  public Transceiver readUsingAuthId(final String authId) throws IOException {
//
//    return new EntityTransceiver<UserProfile>() {
//
//      @Override
//      protected UserProfile fetchEntity() {
//
//        UserProfile userProfile = getUserByAuthId(authId);
//
//        if (userProfile == null) {
//          throw new RestClientException("UserProfile not found at authId=" + authId, HttpStatus.NOT_FOUND);
//        }
//
//        return userProfile;
//      }
//
//      @Override
//      protected Object getView(UserProfile userProfile) {
//        return userProfile;
//      }
//    };
//  }

  /**
   * {@inheritDoc}
   */
  //@Override
//  public UserProfile createUserLogin(final String authId, final UserLogin userLogin) {
//    Preconditions.checkNotNull(userLogin);
//
//    UserProfile userProfile = getUserByAuthId(authId);
//    if (userProfile == null) {
//      throw new RestClientException("UserProfile not found for authId " + authId, HttpStatus.NOT_FOUND);
//    }
//
//    if (this.advancedLogging) {
//      userLogin.setUserProfileID(userProfile.getID());
//      hibernateTemplate.save(userLogin);
//    }
//
//    return userProfile;
//  }

  /**
   * Find the UserProfile object for a given authId
   * @param authId authId
   * @return UserProfile object
   */
//  private UserProfile getUserByAuthId(String authId) {
//    UserProfile userProfile = (UserProfile) DataAccessUtils.uniqueResult(
//        hibernateTemplate.findByCriteria(
//            DetachedCriteria
//                .forClass(UserProfile.class)
//                .add(Restrictions.eq("authId", authId))
//        )
//    );
//
//    return userProfile;
//  }

}