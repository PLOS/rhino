package org.ambraproject.rhino.view.article;

public class SyndicationInputView {

  private String target;
  private String status;
  private String errorMessage;
  private String queue;

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
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

  public String getQueue() {
    return queue;
  }

  public void setQueue(String queue) {
    this.queue = queue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SyndicationInputView that = (SyndicationInputView) o;

    if (target != null ? !target.equals(that.target) : that.target != null) return false;
    if (status != null ? !status.equals(that.status) : that.status != null) return false;
    if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null)
      return false;
    return queue != null ? queue.equals(that.queue) : that.queue == null;

  }

  @Override
  public int hashCode() {
    int result = target != null ? target.hashCode() : 0;
    result = 31 * result + (status != null ? status.hashCode() : 0);
    result = 31 * result + (errorMessage != null ? errorMessage.hashCode() : 0);
    result = 31 * result + (queue != null ? queue.hashCode() : 0);
    return result;
  }
}
