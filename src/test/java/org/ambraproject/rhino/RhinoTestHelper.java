/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2014 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.ambraproject.models.Article;
import org.ambraproject.models.Journal;
import org.ambraproject.models.Syndication;
import org.apache.commons.io.IOUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Miscellaneous fields and methods used in Rhino tests.
 */
public final class RhinoTestHelper {

  private RhinoTestHelper() {}

  /**
   * Mock input stream that yields a constant string and keeps track of whether it has been closed.
   * <p/>
   * Closing the stream is not significant in this implementation, but one might want to test for it.
   */
  public static class TestInputStream extends ByteArrayInputStream {
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
  public static class TestFile {
    private final File fileLocation;
    private final byte[] fileData;

    public TestFile(File fileLocation) throws IOException {
      this.fileLocation = fileLocation;
      InputStream stream = null;
      boolean threw = true;
      try {
        stream = new FileInputStream(this.fileLocation);
        fileData = IOUtils.toByteArray(stream);
        threw = false;
      } finally {
        Closeables.close(stream, threw);
      }
    }

    public TestInputStream read() {
      return new TestInputStream(fileData);
    }

    public byte[] getData() {
      return fileData.clone();
    }
  }

  public static final ImmutableList<String> SAMPLE_ARTICLES = ImmutableList.copyOf(new String[]{
      "pone.0038869",
      // More can be filled in here
  });

  /*
   * Each of these must belong to an article in SAMPLE_ARTICLES.
   */
  private static final ImmutableList<String> SAMPLE_ASSETS = ImmutableList.copyOf(new String[]{
      "pone.0038869.g002.tif",
      // More can be filled in here
  });

  private static final Pattern ASSET_PATTERN = Pattern.compile("((.*)\\.[^.]+?)\\.([^.]+?)");

  public static final String prefixed(String doi) {
    return "10.1371/journal." + doi;
  }

  public static void deleteEntities(HibernateTemplate hibernateTemplate) {
    Collection<Class<?>> typesToDelete = ImmutableList.<Class<?>>of(Article.class, Syndication.class);
    for (Class<?> typeToDelete : typesToDelete) {
      List<?> allObjects = hibernateTemplate.findByCriteria(DetachedCriteria.forClass(typeToDelete));
      hibernateTemplate.deleteAll(allObjects);
    }
  }

  public static Object[][] sampleArticles() {
    List<Object[]> cases = Lists.newArrayListWithCapacity(SAMPLE_ARTICLES.size());
    for (String doiStub : SAMPLE_ARTICLES) {
      Object[] sampleArticle = {
          prefixed(doiStub),
          new File("src/test/resources/articles/" + doiStub + ".xml"),
      };
      cases.add(sampleArticle);
    }
    return cases.toArray(new Object[cases.size()][]);
  }

  public static Object[][] sampleAssets() {
    List<Object[]> cases = Lists.newArrayListWithCapacity(SAMPLE_ASSETS.size());
    for (String assetFileName : SAMPLE_ASSETS) {
      Matcher matcher = ASSET_PATTERN.matcher(assetFileName);
      if (!matcher.matches()) {
        throw new RuntimeException("Asset DOI does not match expected format");
      }
      String assetDoi = matcher.group(1);
      String articleDoi = matcher.group(2);
      String fileExtension = matcher.group(3);
      File articleFile = new File(String.format("src/test/resources/articles/%s.xml", articleDoi));
      File assetFile = new File(String.format("src/test/resources/articles/%s.%s",
          assetDoi, fileExtension));
      cases.add(new Object[]{prefixed(articleDoi), articleFile, prefixed(assetDoi), assetFile});
    }
    return cases.toArray(new Object[cases.size()][]);
  }

  /**
   * Create a dummy journal with required non-null fields filled in.
   *
   * @param eissn the dummy journal's eIssn
   * @return a new dummy journal object
   */
  public static Journal createDummyJournal(String eissn) {
    Preconditions.checkNotNull(eissn);
    Journal journal = new Journal();
    String title = "Test Journal " + eissn;
    journal.setTitle(title);
    journal.setJournalKey(title.replaceAll("\\s|-", ""));
    journal.seteIssn(eissn);
    return journal;
  }

  public static TestInputStream alterStream(InputStream stream, String from, String to) throws IOException {
    String content;
    try {
      content = IOUtils.toString(stream, "UTF-8");
    } finally {
      stream.close();
    }
    content = content.replace(from, to);
    return TestInputStream.of(content);
  }

  public static void addExpectedJournals(HibernateTemplate hibernateTemplate) {
    final ImmutableSet<String> testCaseEissns = ImmutableSet.of("1932-6203");

    for (String eissn : testCaseEissns) {
      List<?> existing = hibernateTemplate.findByCriteria(DetachedCriteria
          .forClass(Journal.class)
          .add(Restrictions.eq("eIssn", eissn)));
      if (!existing.isEmpty())
        continue;
      Journal journal = createDummyJournal(eissn);
      hibernateTemplate.save(journal);
    }
  }
}
