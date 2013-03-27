package org.ambraproject.rhino.util.response;

import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletResponse;

/**
 * A {@link java.io.Writer} container for writing JSON code. Sets the media type for JSON.
 */
public class ServletJsonReceiver extends ServletResponseReceiver {

  public ServletJsonReceiver(HttpServletResponse response) {
    super(response);
    response.setContentType(MediaType.APPLICATION_JSON.toString());
  }

}
