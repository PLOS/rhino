package org.ambraproject.rhino.test;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.test.casetype.ExpectedArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * Temporary, for tinkering with the test case generation. TODO Remove
 */
public class IngestionMetatest {
  private static final Logger log = LoggerFactory.getLogger(IngestionMetatest.class);

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
      .setExclusionStrategies(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
          return false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
          return clazz.isAssignableFrom(Class.class);
        }
      })
      .create();

  public static class Bean {
    public String foo;
  }

  @Test
  public void test() {
    Article fromJson = GSON.fromJson(TEST_JSON, Article.class);

    ExpectedArticle original = new ExpectedArticle();
    original.setTitle("Test!");
    String json = GSON.toJson(original);
    log.debug(json);
    ExpectedArticle persisted = GSON.fromJson(json, ExpectedArticle.class);
    assertFalse(persisted == original);
    assertEquals(persisted, original);
  }

  private static final String TEST_JSON = ""; // Sample case from Admin was pasted here -- still tinkering

}
