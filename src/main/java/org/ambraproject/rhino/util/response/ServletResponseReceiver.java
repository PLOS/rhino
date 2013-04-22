package org.ambraproject.rhino.util.response;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * A container that yields a {@link Writer} from an {@link HttpServletResponse}. This allows services to stream text
 * efficiently to the response while being agnostic about where the {@link Writer} comes from.
 */
public class ServletResponseReceiver implements ResponseReceiver {

  private final HttpServletResponse response;
  private final String prefix;
  private final String suffix;
  private PrintWriter writer;

  /**
   * @param response the response object to write to
   * @param prefix   a string to print before anything printed to the writer, or {@code null} for none
   * @param suffix   a string to print after anything printed to the writer, or {@code null} for none
   */
  protected ServletResponseReceiver(HttpServletResponse response, String prefix, String suffix) {
    this.response = Preconditions.checkNotNull(response);
    this.prefix = Strings.nullToEmpty(prefix);
    this.suffix = Strings.nullToEmpty(suffix);
    this.writer = null;
  }

  @Override
  public void setCharacterEncoding(Charset characterEncoding) {
    response.setCharacterEncoding(characterEncoding.name());
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    if (writer != null) {
      return writer; // Prevent printing the prefix more than once
    }
    PrintWriter responseWriter = response.getWriter();
    writer = new PrintWriter(responseWriter) {
      @Override
      public void close() {
        print(suffix);
        super.close();
      }
    };
    writer.print(prefix);
    return writer;
  }


  /**
   * Wrap a response for writing JSON or JSONP.
   *
   * @param request  the request that may specify a callback for JSONP
   * @param response the response object to write to
   * @return the wrapped response object
   */
  public static ServletResponseReceiver createForJson(HttpServletRequest request,
                                                      HttpServletResponse response) {
    String jsonpCallback = request.getParameter("callback");
    MediaType mediaType;
    String prefix, suffix;
    if (jsonpCallback == null) {
      mediaType = MediaType.JSON_UTF_8;
      prefix = "";
      suffix = "\n";
    } else {
      mediaType = MediaType.JAVASCRIPT_UTF_8;
      prefix = jsonpCallback + '(';
      suffix = ")\n";
    }

    response.setContentType(mediaType.toString());
    return new ServletResponseReceiver(response, prefix, suffix);
  }

}
