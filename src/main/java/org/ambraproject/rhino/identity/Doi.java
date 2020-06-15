/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
    /**
     * Formerly the standard Ambra style. Required by some legacy code.
     */
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

    /**
     * Currently Crossref's standard style.
     */
    HTTPS_DOI_RESOLVER("https://doi.org/") {
      @Override
      protected URI convert(String doiName) throws URISyntaxException, MalformedURLException {
        return new URL("https", "doi.org", "/" + doiName).toURI();
      }
    },

    HTTP_DOI_RESOLVER("http://doi.org/") {
      @Override
      protected URI convert(String doiName) throws URISyntaxException, MalformedURLException {
        return new URL("http", "doi.org", "/" + doiName).toURI();
      }
    },

    HTTPS_DX_RESOLVER("https://dx.doi.org/") {
      @Override
      protected URI convert(String doiName) throws URISyntaxException, MalformedURLException {
        return new URL("https", "dx.doi.org", "/" + doiName).toURI();
      }
    },

    HTTP_DX_RESOLVER("http://dx.doi.org/") {
      @Override
      protected URI convert(String doiName) throws URISyntaxException, MalformedURLException {
        return new URL("http", "dx.doi.org", "/" + doiName).toURI();
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

  private static final ImmutableSet<String> PREFIXES = EnumSet.allOf(UriStyle.class).stream()
      .map(s -> s.prefix).collect(ImmutableSet.toImmutableSet());


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
