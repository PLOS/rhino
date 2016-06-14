package org.ambraproject.rhino.identity;

import com.google.common.collect.ImmutableSet;

import java.net.URI;
import java.net.URISyntaxException;

public final class Doi {

  private final String doiName;

  private Doi(String doiName) {
    this.doiName = sanitize(doiName);
  }

  public static Doi create(String doiName) {
    return new Doi(doiName);
  }

  private static final ImmutableSet<String> PREFIXES = ImmutableSet.of("info:doi/", "doi:");

  private static String sanitize(String doiName) {
    for (String prefix : PREFIXES) {
      if (doiName.startsWith(prefix)) {
        return doiName.substring(prefix.length());
      }
    }
    return doiName;
  }

  public String getName() {
    return doiName;
  }

  public URI getUri() {
    try {
      return new URI("info", "doi/" + doiName, null);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return "Doi{" +
        "doiName='" + doiName + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o != null && getClass() == o.getClass() && doiName.equals(((Doi) o).doiName);
  }

  @Override
  public int hashCode() {
    return doiName.hashCode();
  }

}
