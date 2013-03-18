package org.ambraproject.rhino.test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

public class DummyResponse implements HttpServletResponse {

  private final StringWriter body = new StringWriter();
  private String characterEncoding;

  @Override
  public PrintWriter getWriter() throws IOException {
    return new PrintWriter(body);
  }

  public String read() {
    return body.toString();
  }

  @Override
  public String getCharacterEncoding() {
    return characterEncoding;
  }

  @Override
  public void setCharacterEncoding(String characterEncoding) {
    this.characterEncoding = characterEncoding;
  }


  // Unsupported method implementations

  /**
   * @deprecated Not implemented for dummy class
   * @throws UnsupportedOperationException always
   */
  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void addCookie(Cookie cookie) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public boolean containsHeader(String name) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public String encodeURL(String url) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public String encodeRedirectURL(String url) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public String encodeUrl(String url) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public String encodeRedirectUrl(String url) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void sendError(int sc, String msg) throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void sendError(int sc) throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void sendRedirect(String location) throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void setDateHeader(String name, long date) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void addDateHeader(String name, long date) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void setHeader(String name, String value) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void addHeader(String name, String value) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void setIntHeader(String name, int value) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void addIntHeader(String name, int value) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void setStatus(int sc) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void setStatus(int sc, String sm) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public String getContentType() {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void setContentLength(int len) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void setContentType(String type) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void setBufferSize(int size) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public int getBufferSize() {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void flushBuffer() throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void resetBuffer() {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public boolean isCommitted() {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void reset() {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public void setLocale(Locale loc) {
    throw new UnsupportedOperationException();
  }

  /**
   * @throws UnsupportedOperationException always
   * @deprecated Not implemented for dummy class
   */
  @Deprecated
  @Override
  public Locale getLocale() {
    throw new UnsupportedOperationException();
  }

}
