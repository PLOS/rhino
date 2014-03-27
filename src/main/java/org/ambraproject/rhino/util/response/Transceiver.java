package org.ambraproject.rhino.util.response;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;
import com.google.gson.Gson;
import org.ambraproject.rhombat.HttpDateUtil;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * This class encapsulates data returned from the service layer and presents it to the controller layer.
 * <p/>
 * This class bridges a gap between the two layers and fills a different role for each. To the service layer, this class
 * is an interface that defines callbacks (the {@code protected abstract} methods) for such operations as retrieving an
 * object from persistence or looking up a last-modified timestamp (and maybe more in the future). To the controller
 * layer, this class is a utility that (using the {@link #respond} method) parses a RESTful request and formats the
 * response. The service layer should never need to see types like {@link HttpServletRequest} and {@link
 * HttpServletResponse}, and the controller layer should never need to see the actual data entities being served.
 * <p/>
 * The class name uses the metaphor of a "transceiver" because of this dual role: in the service layer, it
 * <em>receives</em> data from persistence; in the controller layer, it <em>transmits</em> responses to the client.
 * <p/>
 * Generally, one instance of this class is constructed (in the service layer) per service call. For example, there is a
 * call to read an article's metadata, so the service constructs a {@code Transceiver} object that reads the metadata
 * for that particular article. Typically this would be done by passing the relevant identifier to the subclass's
 * constructor or, more often, referring to it closure-style from inside an anonymous subclass.
 */
public abstract class Transceiver {

  /**
   * Get an object representing a piece of metadata.
   *
   * @return the object
   * @throws IOException
   */
  protected abstract Object getMetadata() throws IOException;

  /**
   * Return the last-modified date for this object's data. May return {@code null} for volatile data that always may be
   * assumed to have been modified since it was last accessed, or if looking up the last-modified date is unsupported.
   *
   * @return the date
   * @throws IOException
   */
  protected abstract Calendar getLastModifiedDate() throws IOException;

  private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

  /**
   * Copy a {@link Date} object to a {@link Calendar} object.
   *
   * @param date the {@link Date} object
   * @return an equivalent {@link Calendar} object
   */
  protected static Calendar copyToCalendar(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeZone(GMT);
    calendar.setTime(date);
    return calendar;
  }

  /**
   * Respond with data according to a request.
   * <p/>
   * If the request contains a "Last-Modified" header and the value in question has not been modified since the given
   * date, respond with a 304 ("Not Modified") status code and omit the rest of the response.
   * <p/>
   * If the request provides a JSONP callback parameter, use it to provide a JSONP response.
   *
   * @param request  a service request
   * @param response the object that will receive the response
   * @param gson     an object containing the context's rules for serializing data to responses
   * @throws IOException
   */
  public final void respond(HttpServletRequest request, HttpServletResponse response, Gson gson) throws IOException {
    Preconditions.checkNotNull(request);
    Preconditions.checkNotNull(response);
    Preconditions.checkNotNull(gson);

    Calendar lastModified = getLastModifiedDate();
    if (lastModified != null) {
      response.addHeader("Last-Modified", HttpDateUtil.format(lastModified));
    }
    if (lastModified != null && !checkIfModifiedSince(request, lastModified)) {
      response.setStatus(HttpStatus.NOT_MODIFIED.value());
    } else {
      Object responseData = Preconditions.checkNotNull(getMetadata());
      if (bufferResponseBody()) {
        serializeMetadataSafely(request, response, gson, responseData);
      } else {
        serializeMetadataDirectly(request, response, gson, responseData);
      }
    }
  }

  /**
   * Dump this object's response directly to a string as JSON. For testing only.
   *
   * @param gson an object containing the context's rules for serializing data to responses
   * @return the response body as a JSON string
   * @throws IOException
   */
  @VisibleForTesting
  public String readJson(Gson gson) throws IOException {
    return gson.toJson(getMetadata());
  }

  /**
   * Checks for the presence of an "If-Modified-Since" header on the request.  If it exists, returns true iff
   * lastModified is after the date in the header.  That is, returns true if we should send content, and false if a 304
   * Not Modified response is appropriate.
   *
   * @param request      HttpServletRequest
   * @param lastModified last modified date of the entity being requested
   * @return true if we should send the entity, false if we should send a 304 response
   */
  private boolean checkIfModifiedSince(HttpServletRequest request, Calendar lastModified) {
    String ifModifiedSince = request.getHeader("If-Modified-Since");
    if (Strings.isNullOrEmpty(ifModifiedSince)) {
      return true;
    } else {
      Calendar headerCal = HttpDateUtil.parse(ifModifiedSince);
      return lastModified.after(headerCal);
    }
  }

  /**
   * @return {@code true} if responses should be buffered fully to memory before opening a response stream
   * @see #serializeMetadataSafely
   * @see #serializeMetadataDirectly
   */
  // TODO: Make configurable (which is tricky because Transceivers aren't Spring beans)
  protected boolean bufferResponseBody() {
    return true;
  }

  private static final int JSON_BUFFER_INITIAL_SIZE = 0x400;


  /**
   * Buffer the JSON into memory before we open the response stream. This ensures that any exception thrown by {@code
   * toJson} and caught by Spring will be correctly shown in the response to the client.
   */
  private void serializeMetadataSafely(HttpServletRequest request, HttpServletResponse response, Gson gson, Object responseData)
      throws IOException {
    StringWriter stringWriter = new StringWriter(JSON_BUFFER_INITIAL_SIZE);
    gson.toJson(responseData, stringWriter);

    try (PrintWriter writer = openJsonResponseBody(request, response)) {
      writer.write(stringWriter.toString());
    }
  }

  /**
   * Write the JSON directly to the response stream. This improves performance if the JSON is long, but risks ungraceful
   * degradation. Specifically, if {@code toJson} throws an exception, Spring will silently error out on the response
   * (probably sending an "OK" status code but an empty response body) instead of correctly reporting it to the client
   * as a 500 error and providing the stack trace.
   */
  private void serializeMetadataDirectly(HttpServletRequest request, HttpServletResponse response, Gson gson, Object responseData)
      throws IOException {
    try (PrintWriter writer = openJsonResponseBody(request, response)) {
      gson.toJson(responseData, writer);
    }
  }

  private static final String CALLBACK_PARAM_NAME = "callback";

  /**
   * Set the response's content type and open a writer for the response body. Apply JSONP if the request specifies it.
   */
  private static PrintWriter openJsonResponseBody(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    final String callbackParamValue = request.getParameter(CALLBACK_PARAM_NAME);
    response.setCharacterEncoding(Charsets.UTF_8.name());
    if (callbackParamValue == null) {
      // Respond with plain JSON
      response.setContentType(MediaType.JSON_UTF_8.toString());
      return response.getWriter();
    } else {
      // Respond with JSONP
      response.setContentType(MediaType.JAVASCRIPT_UTF_8.toString());
      return new JsonpPrintWriter(response.getWriter(), callbackParamValue);
    }
  }

  /**
   * Writer that surrounds everything printed to it with a JSONP function call.
   */
  private static class JsonpPrintWriter extends PrintWriter {
    private boolean isClosed;

    private JsonpPrintWriter(PrintWriter delegate, String callbackParam) {
      super(delegate);
      isClosed = false;

      try {
        // Open JSONP function call
        print(callbackParam);
        print('(');
      } catch (Throwable t) {
        // Ensure that the delegate stream (typically a client response) is closed even if the initial print fails.
        // Because we're still in the constructor, the outer object won't be reachable in a 'finally' block yet.
        close();
        throw t;
      }
    }

    @Override
    public void close() {
      try {
        if (!isClosed) {
          // Close JSONP function call (but only the first time it's closed)
          println(')');
          isClosed = true;
        }
      } finally {
        super.close();
      }
    }
  }

}

