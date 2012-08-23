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
import java.util.Arrays;
import java.util.List;

@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = TestConfiguration.class)
public abstract class BaseAdminTest extends AbstractTestNGSpringContextTests {

  @Autowired
  protected HibernateTemplate hibernateTemplate;

  /**
   * Mock input stream that yields a constant string and keeps track of whether is has been closed.
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
      return new TestInputStream(Arrays.copyOf(data, data.length));
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
        IOUtils.closeQuietly(stream);
      }
    }

    public TestInputStream read() {
      return new TestInputStream(fileData);
    }

    public byte[] getData() {
      return Arrays.copyOf(fileData, fileData.length);
    }
  }

  protected static final ImmutableList<String> SAMPLE_ARTICLES = ImmutableList.copyOf(new String[]{
      "journal.pone.0038869",
  });

  @DataProvider
  public Object[][] sampleArticles() {
    List<Object[]> cases = Lists.newArrayListWithCapacity(SAMPLE_ARTICLES.size());
    for (String doiStub : SAMPLE_ARTICLES) {
      Object[] sampleArticle = {
          "info:doi/10.1371/" + doiStub,
          new File("src/test/resources/data/" + doiStub + ".xml"),
      };
      cases.add(sampleArticle);
    }
    return cases.toArray(new Object[cases.size()][]);
  }

}
