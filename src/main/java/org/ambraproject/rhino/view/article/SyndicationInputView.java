package org.ambraproject.rhino.view.article;

public class SyndicationInputView {

  private String targetQueue;
  private String status;
  private String errorMessage;

  public String getTargetQueue() {
    return targetQueue;
  }

  public void setTargetQueue(String targetQueue) {
    this.targetQueue = targetQueue;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SyndicationInputView that = (SyndicationInputView) o;

    if (status != null ? !status.equals(that.status) : that.status != null) return false;
    if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null)
      return false;
    return targetQueue != null ? targetQueue.equals(that.targetQueue) : that.targetQueue == null;

  }

  @Override
  public int hashCode() {
    int result = targetQueue != null ? targetQueue.hashCode() : 0;
    result = 31 * result + (status != null ? status.hashCode() : 0);
    result = 31 * result + (errorMessage != null ? errorMessage.hashCode() : 0);
    return result;
  }
}
