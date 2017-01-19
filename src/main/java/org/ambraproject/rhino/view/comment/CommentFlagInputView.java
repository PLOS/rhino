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

package org.ambraproject.rhino.view.comment;

public class CommentFlagInputView {

  private String creatorUserId;
  private String body;
  private String reasonCode;

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public CommentFlagInputView() {
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setCreatorUserId(String creatorUserId) {
    this.creatorUserId = creatorUserId;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setBody(String body) {
    this.body = body;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setReasonCode(String reasonCode) {
    this.reasonCode = reasonCode;
  }

  public String getCreatorUserId() {
    return creatorUserId;
  }

  public String getBody() {
    return body;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CommentFlagInputView)) return false;

    CommentFlagInputView that = (CommentFlagInputView) o;

    if (body != null ? !body.equals(that.body) : that.body != null) return false;
    if (creatorUserId != null ? !creatorUserId.equals(that.creatorUserId) : that.creatorUserId != null) return false;
    if (reasonCode != null ? !reasonCode.equals(that.reasonCode) : that.reasonCode != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = creatorUserId != null ? creatorUserId.hashCode() : 0;
    result = 31 * result + (body != null ? body.hashCode() : 0);
    result = 31 * result + (reasonCode != null ? reasonCode.hashCode() : 0);
    return result;
  }
}
