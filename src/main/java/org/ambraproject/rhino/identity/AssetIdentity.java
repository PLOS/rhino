package org.ambraproject.rhino.identity;

public class AssetIdentity extends DoiBasedIdentity {

  protected AssetIdentity(String identifier) {
    super(identifier);
  }

  public static AssetIdentity create(String identifier) {
    return new AssetIdentity(identifier);
  }

}
