package org.ambraproject.rhino.identity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.EnumSet;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A Digital Object Identifier.
 * <p>
 * This class does not validate that the DOI has a valid prefix. A server operated by a publisher who does not have a
 * registered DOI prefix is free to use any string in place of resolvable DOIs (in which case it's up to their front end
 * to avoid presenting them as such).
 * <p>
 * DOIs are <a href="https://www.doi.org/doi_handbook/2_Numbering.html#2.4">case-insensitive by specification.</a>
 */
public final class Doi {

  public static enum UriStyle {
    INFO_DOI("info:doi/") {
      @Override
      protected URI convert(String doiName) throws URISyntaxException {
        return new URI("info", "doi/" + doiName, null);
      }
    },
    DOI_SCHEME("doi:") {
      @Override
      protected URI convert(String doiName) throws URISyntaxException {
        return new URI("doi", doiName, null);
      }
    },
    HTTP_RESOLVER("http://dx.doi.org/") {
      @Override
      protected URI convert(String doiName) throws URISyntaxException, MalformedURLException {
        return new URL("http", "dx.doi.org/", doiName).toURI();
      }
    },
    HTTPS_RESOLVER("https://dx.doi.org/") {
      @Override
      protected URI convert(String doiName) throws URISyntaxException, MalformedURLException {
        return new URL("https", "dx.doi.org/", doiName).toURI();
      }
    };

    private final String prefix;

    private UriStyle(String prefix) {
      this.prefix = prefix;
    }

    @VisibleForTesting
    String getPrefix() {
      return prefix;
    }

    protected abstract URI convert(String doiName) throws URISyntaxException, MalformedURLException;
  }

  private static final ImmutableSet<String> PREFIXES = ImmutableSet.copyOf(
      EnumSet.allOf(UriStyle.class).stream().map(s -> s.prefix).collect(Collectors.toList()));


  private final String doiName;

  private Doi(String doiName) {
    this.doiName = sanitize(doiName);
  }

  public static Doi create(String doiName) {
    return new Doi(doiName);
  }

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

  public URI asUri(UriStyle style) {
    try {
      return style.convert(doiName);
    } catch (URISyntaxException | MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return "Doi{" +
        "doiName='" + doiName + '\'' +
        '}';
  }

  /**
   * Two DOIs are equal if their DOI names are <em>case-insensitively</em> equal.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    // Technically the spec says that only ASCII characters should be compared case-insensitively,
    // but this is close enough and consistent with the equality definition at the database layer.
    return doiName.equalsIgnoreCase(((Doi) o).doiName);
  }

  @Override
  public int hashCode() {
    // Must produce equal hashes for case-insensitively equal names
    return doiName.toLowerCase(Locale.ROOT).hashCode();
  }

  public static final JsonSerializer<Doi> SERIALIZER = (doi, typeOfSrc, context)
      -> new JsonPrimitive(doi.getName());
}
