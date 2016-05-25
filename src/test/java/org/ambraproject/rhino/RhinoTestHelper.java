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
import com.google.gson.Gson;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleAsset;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.DoiBasedCrudService;
import org.ambraproject.rhino.util.Archive;
import org.apache.commons.io.IOUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Miscellaneous fields and methods used in Rhino tests.
 */
public final class RhinoTestHelper {

  private RhinoTestHelper() {
  }

  /**
   * Mock input stream that yields a constant string and keeps track of whether it has been closed.
   * <p>
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

    public TestFile(File fileLocation) {
      this.fileLocation = fileLocation;
      boolean threw = true;
      try (InputStream stream = new FileInputStream(this.fileLocation)) {
        fileData = IOUtils.toByteArray(stream);
      } catch (IOException e) {
        throw new RuntimeException(e);
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

  private static File getXmlPath(String doiStub) {
    return new File("src/test/resources/articles/" + doiStub + ".xml");
  }

  private static File getJsonPath(String doiStub) {
    return new File("src/test/resources/articles/" + doiStub + ".json");
  }

  public static Object[][] sampleArticles() {
    List<Object[]> cases = Lists.newArrayListWithCapacity(SAMPLE_ARTICLES.size());
    for (String doiStub : SAMPLE_ARTICLES) {
      Object[] sampleArticle = {prefixed(doiStub), getXmlPath(doiStub), getJsonPath(doiStub)};
      cases.add(sampleArticle);
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
      if (!existing.isEmpty()) {
        continue;
      }
      Journal journal = createDummyJournal(eissn);
      hibernateTemplate.save(journal);
    }
  }

  public static Archive createMockIngestible(ArticleIdentity articleId, InputStream xmlData,
                                             List<ArticleAsset> referenceAssets) {
    try {
      try {
        String archiveName = articleId.getLastToken() + ".zip";
        InputStream mockIngestible = IngestibleUtil.buildMockIngestible(xmlData, referenceAssets);
        return Archive.readZipFileIntoMemory(archiveName, mockIngestible);
      } finally {
        xmlData.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Stream<Article> createTestArticles(ArticleCrudService articleCrudService) {
    return SAMPLE_ARTICLES.stream().map(doiStub -> createTestArticle(articleCrudService, doiStub));
  }

  public static Article createTestArticle(ArticleCrudService articleCrudService) {
    return createTestArticle(articleCrudService, SAMPLE_ARTICLES.get(0));
  }

  public static Article createTestArticle(ArticleCrudService articleCrudService, String doiStub) {
    ArticleIdentity articleId = ArticleIdentity.create(RhinoTestHelper.prefixed(doiStub));
    RhinoTestHelper.TestFile sampleFile = new RhinoTestHelper.TestFile(getXmlPath(doiStub));
    String doi = articleId.getIdentifier();

    byte[] sampleData;
    try {
      sampleData = IOUtils.toByteArray(RhinoTestHelper.alterStream(sampleFile.read(), doi, doi));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    Article reference = readReferenceCase(getJsonPath(doiStub));

    RhinoTestHelper.TestInputStream input = RhinoTestHelper.TestInputStream.of(sampleData);
    Archive mockIngestible = createMockIngestible(articleId, input, reference.getAssets());
    try {
      return articleCrudService.writeArchive(mockIngestible,
          Optional.of(articleId), DoiBasedCrudService.WriteMode.CREATE_ONLY, OptionalInt.empty());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Article readReferenceCase(File jsonFile) {
    Preconditions.checkNotNull(jsonFile);
    Article article;
    try (Reader input = new BufferedReader(new FileReader(jsonFile))) {
      article = new Gson().fromJson(input, Article.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return article;
  }

}
