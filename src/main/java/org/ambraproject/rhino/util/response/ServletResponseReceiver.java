package org.ambraproject.rhino.util.response;

import com.google.common.base.Preconditions;

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

  public ServletResponseReceiver(HttpServletResponse response) {
    this.response = Preconditions.checkNotNull(response);
  }

  @Override
  public void setCharacterEncoding(Charset characterEncoding) {
    response.setCharacterEncoding(characterEncoding.name());
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return response.getWriter();
  }

}
