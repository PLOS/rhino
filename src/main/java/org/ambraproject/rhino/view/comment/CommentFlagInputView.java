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
