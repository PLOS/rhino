package org.ambraproject.rhino.service;


import com.google.common.collect.Lists;
import org.ambraproject.rhino.BaseRhinoTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * This is actually a test on {@link ArticleCrudService#write} and would normally be in {@link ArticleCrudServiceTest}.
 * But because of the special way that it gathers its assertion data, I'm putting it in its own class now. Maybe it
 * should be moved into the regular test when it's ready to supplant/replace it.
 * <p/>
 * The test case data for this class live at {@code DATA_PATH}. The XML file is raw article data as it would be passed
 * to the ingester in production. The matching JSON file is a Gson dump of an {@link org.ambraproject.models.Article}
 * instance as created by the reference implementation ({@code org.ambraproject.article.service.IngesterImpl}).
 */
public class IngestionTest extends BaseRhinoTest {
  private static final Logger log = LoggerFactory.getLogger(IngestionTest.class);

  private static final File DATA_PATH = new File("src/test/resources/data/ingestcase/");
  private static final String JSON_SUFFIX = ".json";
  private static final String XML_SUFFIX = ".xml";

  private static FilenameFilter forSuffix(final String suffix) {
    return new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(suffix);
      }
    };
  }

  @DataProvider
  public Object[][] generatedIngestionData() {
    File[] jsonFiles = DATA_PATH.listFiles(forSuffix(JSON_SUFFIX));
    List<Object[]> cases = Lists.newArrayListWithCapacity(jsonFiles.length);
    for (File jsonFile : jsonFiles) {
      String jsonFilePath = jsonFile.getPath();
      String xmlPath = jsonFilePath.substring(0, jsonFilePath.length() - JSON_SUFFIX.length()) + XML_SUFFIX;
      File xmlFile = new File(xmlPath);
      if (!xmlFile.exists()) {
        fail("No XML file to match JSON test case data: " + xmlPath);
      }
      cases.add(new Object[]{jsonFile, xmlFile});
    }
    return cases.toArray(new Object[0][]);
  }

  @Test(dataProvider = "generatedIngestionData")
  public void testIngestion(File jsonFile, File xmlFile) {
    assertTrue(jsonFile.exists()); // placeholder; TODO implement test
    assertTrue(xmlFile.exists()); // placeholder; TODO implement test
  }

}
