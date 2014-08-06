package org.ambraproject.rhino.util.response;

import com.google.common.collect.ImmutableList;
import org.ambraproject.models.Article;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class EntityCollectionTransceiverTest {

  private static Calendar makeCalendarWithOffset(int offset) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(offset);
    calendar = Transceiver.copyToCalendar(calendar.getTime()); // coerce to GMT
    return calendar;
  }

  private static Article makeArticleWithTimestamp(int offset) {
    Article article = new Article();
    article.setLastModified(makeCalendarWithOffset(offset).getTime());
    return article;
  }

  @Test
  public void testGetLastModifiedDate() throws IOException {
    final List<Article> articles = ImmutableList.of(
        makeArticleWithTimestamp(0),
        makeArticleWithTimestamp(-1),
        makeArticleWithTimestamp(2),
        makeArticleWithTimestamp(1)
    );

    // Extend to test EntityCollectionTransceiver.getLastModifiedDate()
    Transceiver mockTransceiver = new EntityCollectionTransceiver<Article>() {
      @Override
      protected Collection<? extends Article> fetchEntities() {
        return articles;
      }

      @Override
      protected Object getView(Collection<? extends Article> entities) {
        throw new UnsupportedOperationException();
      }
    };

    Calendar expected = makeCalendarWithOffset(2);
    Calendar actual = mockTransceiver.getLastModifiedDate();
    assertEquals(actual, expected);
  }

}
