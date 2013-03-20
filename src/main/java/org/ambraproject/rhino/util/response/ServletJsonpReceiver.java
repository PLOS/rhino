package org.ambraproject.rhino.util.response;

import com.google.common.base.Preconditions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * A {@link java.io.Writer} container for writing JSONP code. Sets the media type for JSON and transparently wraps the
 * written text stream in the JSONP callback call.
 */
public class ServletJsonpReceiver extends ServletJsonReceiver {

  private final String callback;
  private PrintWriter writer;

  private ServletJsonpReceiver(HttpServletResponse response, String callback) {
    super(response);
    this.callback = Preconditions.checkNotNull(callback);
    this.writer = null;
  }

  /**
   * Return a receiver that will selectively provide JSONP if the request provides a callback method name for JSONP.
   * Otherwise, the returned object will provide plain JSON.
   *
   * @param request  an HTTP request for JSON
   * @param response the HTTP response that will receive the JSON
   * @return a receiver object
   */
  public static ServletJsonReceiver create(HttpServletRequest request, HttpServletResponse response) {
    String callback = getCallback(request);
    return (callback == null) ? new ServletJsonReceiver(response) : new ServletJsonpReceiver(response, callback);
  }

  /**
   * Return a receiver that provides JSONP.
   *
   * @param request  an HTTP request that offers a callback parameter for JSONP
   * @param response the HTTP response that will receive the JSONP
   * @return a receiver object
   * @throws IllegalArgumentException if {@code request} does not have a "callback" parameter
   */
  public static ServletJsonpReceiver createForJsonpOnly(HttpServletRequest request, HttpServletResponse response) {
    String callback = getCallback(request);
    if (callback == null) {
      throw new IllegalArgumentException("Request does not have a parameter for JSONP callback");
    }
    return new ServletJsonpReceiver(response, callback);
  }

  private static String getCallback(HttpServletRequest request) {
    return request.getParameter("callback");
  }

  @Override
  public synchronized PrintWriter getWriter() throws IOException {
    if (writer != null) {
      return writer; // Don't print the callback padding more than once
    }

    // Delegate to the response's writer, but surround everything printed to it with the JavaScript method call.
    writer = new PrintWriter(super.getWriter()) {
      {
        print(callback);
        print('(');
      }

      @Override
      public void close() {
        println(')');
        super.close();
      }
    };
    return writer;
  }

}
