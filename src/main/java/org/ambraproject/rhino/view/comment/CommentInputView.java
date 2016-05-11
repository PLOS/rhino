package org.ambraproject.rhino.view.comment;

public class CommentInputView {

  private String articleDoi;
  private String creatorUserId;
  private String parentCommentId;
  private String title;
  private String body;
  private String highlightedText;
  private String competingInterestStatement;

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public CommentInputView() {
  }

  public String getArticleDoi() {
    return articleDoi;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setArticleDoi(String articleDoi) {
    this.articleDoi = articleDoi;
  }

  public String getCreatorUserId() {
    return creatorUserId;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setCreatorUserId(String creatorUserId) {
    this.creatorUserId = creatorUserId;
  }

  public String getParentCommentId() {
    return parentCommentId;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setParentCommentId(String parentCommentId) {
    this.parentCommentId = parentCommentId;
  }

  public String getTitle() {
    return title;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setBody(String body) {
    this.body = body;
  }

  public String getHighlightedText() {
    return highlightedText;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setHighlightedText(String highlightedText) {
    this.highlightedText = highlightedText;
  }

  public String getCompetingInterestStatement() {
    return competingInterestStatement;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setCompetingInterestStatement(String competingInterestStatement) {
    this.competingInterestStatement = competingInterestStatement;
  }

}
