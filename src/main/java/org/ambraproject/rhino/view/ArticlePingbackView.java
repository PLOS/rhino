package org.ambraproject.rhino.view;

import com.google.common.base.Preconditions;

import java.util.Date;

/**
 * A view of an article that contains a digest of its associated pingbacks (and some other data that's friendly to a
 * particular client-side use case).
 */
public class ArticlePingbackView
    implements ArticleView // Don't implement JsonOutputView; Gson's reflection-based serialization suffices
{

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

}
