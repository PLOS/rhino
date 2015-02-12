package org.ambraproject.rhino.service.classifier;

import com.google.common.collect.ImmutableMap;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.Map;

public class DummyArticleClassifier implements ArticleClassifier {

  public static final ImmutableMap<String, Integer> DUMMY_DATA = ImmutableMap.<String, Integer>builder()
      .put("/TopLevel1/term1", 5)
      .put("/TopLevel2/term2", 10)
      .build();

  @Override
  public Map<String, Integer> classifyArticle(Document articleXml) throws IOException {
    return DUMMY_DATA;
  }

}
