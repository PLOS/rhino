package org.ambraproject.rhino.service.taxonomy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.ambraproject.models.Article;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DummyTaxonomyClassificationService implements TaxonomyClassificationService {

  public static final ImmutableMap<String, Integer> DUMMY_DATA = ImmutableMap.<String, Integer>builder()
      .put("/TopLevel1/term1", 5)
      .put("/TopLevel2/term2", 10)
      .build();

  @Override
  public Map<String, Integer> classifyArticle(Document articleXml, Article article) throws IOException {
    return DUMMY_DATA;
  }

  @Override
  public List<String> getRawTerms(Document articleXml, Article article,
                                  boolean isTextRequired) throws IOException {
    if (isTextRequired) {
      return ImmutableList.of("dummy text sent to MAIstro", "dummy raw term");
    }
    return ImmutableList.of("dummy raw term");
  }

}
