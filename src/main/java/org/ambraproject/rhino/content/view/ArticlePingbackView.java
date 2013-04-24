package org.ambraproject.rhino.content.view;

import com.google.common.base.Preconditions;

import java.util.Date;

public class ArticlePingbackView implements ArticleView {

  private final String doi;
  private final String title;
  private final String url;
  private final long pingbackCount;
  private final Date mostRecentPingback;

  public ArticlePingbackView(String doi, String title, String url, long pingbackCount, Date mostRecentPingback) {
    Preconditions.checkArgument(pingbackCount >= 0L);
    Preconditions.checkArgument((mostRecentPingback == null) == (pingbackCount == 0L));
    this.doi = Preconditions.checkNotNull(doi);
    this.title = title;
    this.url = url;
    this.pingbackCount = pingbackCount;
    this.mostRecentPingback = mostRecentPingback;
  }

  @Override
  public String getDoi() {
    return doi;
  }

  /*
   * No JsonSerializer needed; Gson's reflection-based serialization behavior is fine.
   */

}
