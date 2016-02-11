package org.ambraproject.rhino.view.comment;

public class CommentFlagInputView {

  private String creatorAuthId; // keeping for backward compatibility
  private String creatorNedId;
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
  public void setCreatorAuthId(String creatorAuthId) {
    this.creatorAuthId = creatorAuthId;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setCreatorNedId(String creatorNedId) {
    this.creatorNedId = creatorNedId;
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

  public String getCreatorAuthId() {
    return creatorAuthId;
  }

  public String getCreatorNedId() {
    return creatorNedId;
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
    if (creatorNedId != null ? !creatorNedId.equals(that.creatorNedId) : that.creatorNedId != null) return false;
    if (reasonCode != null ? !reasonCode.equals(that.reasonCode) : that.reasonCode != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = creatorNedId != null ? creatorNedId.hashCode() : 0;
    result = 31 * result + (body != null ? body.hashCode() : 0);
    result = 31 * result + (reasonCode != null ? reasonCode.hashCode() : 0);
    return result;
  }
}
