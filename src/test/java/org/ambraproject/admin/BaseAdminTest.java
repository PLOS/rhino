/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.admin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.ambraproject.admin.config.TestConfiguration;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.DataProvider;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = TestConfiguration.class)
public abstract class BaseAdminTest extends AbstractTestNGSpringContextTests {

  @Autowired
  protected HibernateTemplate hibernateTemplate;

  /**
   * Mock input stream that yields a constant string and keeps track of whether it has been closed.
   * <p/>
   * Closing the stream is not significant in this implementation, but one might want to test for it.
   */
  protected static class TestInputStream extends ByteArrayInputStream {
    private boolean isClosed;

    private TestInputStream(byte[] data) {
      super(data);
      isClosed = false;
    }

    public static TestInputStream of(byte[] data) {
      return new TestInputStream(data.clone());
    }

    public static TestInputStream of(String data) {
      return new TestInputStream(data.getBytes());
    }

    public boolean isClosed() {
      return isClosed;
    }

    @Override
    public void close() throws IOException {
      super.close();
      isClosed = true;
    }
  }

  /**
   * A file holding test input. An instance pre-loads the file contents into memory, and then makes them available as
   * needed through {@link TestInputStream}s or array dumps.
   */
  protected static class TestFile {
    private final File fileLocation;
    private final byte[] fileData;

    public TestFile(File fileLocation) throws IOException {
      this.fileLocation = fileLocation;
      InputStream stream = null;
      try {
        stream = new FileInputStream(this.fileLocation);
        fileData = IOUtils.toByteArray(stream);
      } finally {
        Closeables.close(stream, false);
      }
    }

    public TestInputStream read() {
      return new TestInputStream(fileData);
    }

    public byte[] getData() {
      return fileData.clone();
    }
  }

  private static final ImmutableList<String> SAMPLE_ARTICLES = ImmutableList.copyOf(new String[]{
      "journal.pone.0038869",
      // More can be filled in here
  });

  /*
   * Each of these must belong to an article in SAMPLE_ARTICLES.
   */
  private static final ImmutableList<String> SAMPLE_ASSETS = ImmutableList.copyOf(new String[]{
      "journal.pone.0038869.g002.tif",
      // More can be filled in here
  });

  private static final Pattern ASSET_PATTERN = Pattern.compile("((.*)\\.[^.]+?)\\.([^.]+?)");

  private static final String prefixed(String doi) {
    return "10.1371/" + doi;
  }

  @DataProvider
  public Object[][] sampleArticles() {
    List<Object[]> cases = Lists.newArrayListWithCapacity(SAMPLE_ARTICLES.size());
    for (String doiStub : SAMPLE_ARTICLES) {
      Object[] sampleArticle = {
          prefixed(doiStub),
          new File("src/test/resources/data/" + doiStub + ".xml"),
      };
      cases.add(sampleArticle);
    }
    return cases.toArray(new Object[cases.size()][]);
  }

  @DataProvider
  public Object[][] sampleAssets() {
    List<Object[]> cases = Lists.newArrayListWithCapacity(SAMPLE_ASSETS.size());
    for (String assetFileName : SAMPLE_ASSETS) {
      Matcher matcher = ASSET_PATTERN.matcher(assetFileName);
      if (!matcher.matches()) {
        throw new RuntimeException("Asset DOI does not match expected format");
      }
      String assetDoi = matcher.group(1);
      String articleDoi = matcher.group(2);
      String fileExtension = matcher.group(3);
      File articleFile = new File(String.format("src/test/resources/data/%s.xml", articleDoi));
      File assetFile = new File(String.format("src/test/resources/data/%s.%s",
          assetDoi, fileExtension));
      cases.add(new Object[]{prefixed(articleDoi), articleFile, prefixed(assetDoi), assetFile});
    }
    return cases.toArray(new Object[cases.size()][]);
  }

}
