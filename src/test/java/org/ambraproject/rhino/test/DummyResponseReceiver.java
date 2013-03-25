package org.ambraproject.rhino.test;

import org.ambraproject.rhino.util.response.ResponseReceiver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;


public class DummyResponseReceiver implements ResponseReceiver {

  private Charset characterEncoding;
  private StringWriter body = new StringWriter();

  public Charset getCharacterEncoding() {
    return characterEncoding;
  }

  @Override
  public void setCharacterEncoding(Charset characterEncoding) {
    this.characterEncoding = characterEncoding;
  }

  @Override
  public PrintWriter getWriter() throws IOException {
    return new PrintWriter(body);
  }

  public String read() {
    return body.toString();
  }

}
