package org.ambraproject.rhino.view.user;

import org.ambraproject.rhino.view.KeyedStringList;

import java.util.Collection;

public class AuthIdList extends KeyedStringList {

  public AuthIdList(Collection<String> authIds) {
    super(authIds);
  }

  @Override
  protected String extractIdentifier(String value) {
    return value;
  }

  @Override
  protected String getMemberName() {
    return "authId";
  }

}
