package org.ambraproject.rhino.util.response;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * A container for a {@link Writer} that receives a text stream to write to an HTTP response or similar.
 */
public interface ResponseReceiver {

  /**
   * Set the character encoding (MIME charset) to use to write the response.
   *
   * @param characterEncoding the character encoding (non-null)
   */
  public abstract void setCharacterEncoding(Charset characterEncoding);

  /**
   * Get a writer object to which to write the data.
   * <p/>
   * It is important to call this method as late as possible before writing. If it wraps a {@link
   * javax.servlet.http.HttpServletResponse} as is typical, extracting the {@link Writer} prematurely may interfere with
   * Spring handling exceptions. Spring can't print an error message to the response after the response has yielded a
   * {@code Writer} object. This is part of the reason to pass an instance of this class to the service layer rather
   * than the bare {@code Writer}.
   * <p/>
   * It is the caller's responsibility to close the returned {@code Writer}.
   *
   * @return a writer
   * @throws IOException
   */
  public abstract Writer getWriter() throws IOException;

}
