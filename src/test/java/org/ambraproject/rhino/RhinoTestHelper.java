/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Journal;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

/**
 * Miscellaneous fields and methods used in Rhino tests.
 */
public final class RhinoTestHelper {

  private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

  static {
    factory.setNamespaceAware(true);
  }

  private RhinoTestHelper() {}

  /**
   * Parses an XML file.
   * 
   * @param xmlDataResource The XML URL data resource
   *
   * @return The {@link org.w3c.dom.Document Document}
   *
   * @throws ParserConfigurationException if a DocumentBuilder cannot be created which satisfies the
   *         configuration requested
   * @throws IOException if cannot process file
   * @throws SAXException if any parse errors occur
   */
  public static Document loadXMLFromString(URL xmlDataResource)
      throws ParserConfigurationException, IOException, SAXException {
    final File xmlFile = FileUtils.toFile(xmlDataResource);
    final Document document = loadXMLFromString(xmlFile);
    return document;
  }

  /**
   * Parses an XML file.
   * 
   * @param xml The XML file
   *
   * @return The {@link org.w3c.dom.Document Document}
   *
   * @throws ParserConfigurationException if a DocumentBuilder cannot be created which satisfies the
   *         configuration requested
   * @throws IOException if cannot process file
   * @throws SAXException if any parse errors occur
   */
  public static Document loadXMLFromString(File xml)
      throws ParserConfigurationException, IOException, SAXException {
    final DocumentBuilder builder = factory.newDocumentBuilder();
    final byte[] data = FileUtils.readFileToByteArray(xml);
    final Document document = builder.parse(new ByteArrayInputStream(data));
    return document;
  }

  /**
   * Mock input stream that yields a constant string and keeps track of whether it has been closed.
   * <p>
   * Closing the stream is not significant in this implementation, but one might want to test for
   * it.
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
   * A file holding test input. An instance pre-loads the file contents into memory, and then makes
   * them available as needed through {@link TestInputStream}s or array dumps.
   */
  public static class TestFile {
    private final File fileLocation;
    private final byte[] fileData;

    public TestFile(File fileLocation) {
      this.fileLocation = fileLocation;
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

  /**
   * Class to implement a {@link RuntimeConfiguration.ContentRepoEndpoint
   * ContentRepoEndpoint}, which can be used for unit tests.
   */
  public static class TestContentRepoEndpoint
      implements RuntimeConfiguration.ContentRepoEndpoint {
    private String bucket;

    private Optional<URI> address = Optional.empty();

    /**
     * Creates an instance of <code>TestContentRepoEndpoint</code>.
     *
     * @param bucket The default bucket
     * @param address The URI address
     */
    public TestContentRepoEndpoint(String bucket, URI address) {
      this.bucket = bucket;
      this.address = Optional.ofNullable(address);
    }

    /** Returns the URI. */
    @Override
    public URI getAddress() {
      return address.orElse(null);
    }

    /** Returns the default bucket. */
    @Override
    public String getBucket() {
      return bucket;
    }
  }

  public static final ImmutableList<String> SAMPLE_ARTICLES = ImmutableList.of("pone.0038869"
      // More can be filled in here
      );

  /*
   * Each of these must belong to an article in SAMPLE_ARTICLES.
   */
  public static final ImmutableList<String> SAMPLE_ASSETS = ImmutableList.of("pone.0038869.g002.tif"
      // More can be filled in here
  );

  public static final Pattern ASSET_PATTERN = Pattern.compile("((.*)\\.[^.]+?)\\.([^.]+?)");

  public static final String prefixed(String doi) {
    return "10.1371/journal." + doi;
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

  public static TestInputStream alterStream(InputStream stream, String from, String to)
      throws IOException {
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
      List<?> existing = hibernateTemplate.findByCriteria(
          DetachedCriteria.forClass(Journal.class).add(Restrictions.eq("eIssn", eissn)));
      if (!existing.isEmpty()) {
        continue;
      }
      Journal journal = createDummyJournal(eissn);
      // hibernateTemplate.save(journal);
    }
  }

  // public static Archive createMockIngestible(ArticleIdentity articleId, InputStream xmlData,
  // List<ArticleAsset> referenceAssets) {
  // try {
  // try {
  // String archiveName = articleId.getLastToken() + ".zip";
  // InputStream mockIngestible = IngestibleUtil.buildMockIngestible(xmlData, referenceAssets);
  // return Archive.readZipFileIntoMemory(archiveName, mockIngestible);
  // } finally {
  // xmlData.close();
  // }
  // } catch (IOException e) {
  // throw new RuntimeException(e);
  // }
  // }

  // public static Stream<ArticleMetadata> createTestArticles(IngestionService ingestionService) {
  // return SAMPLE_ARTICLES.stream().map(doiStub -> createTestArticle(ingestionService, doiStub));
  // }
  //
  // public static ArticleMetadata createTestArticle(IngestionService ingestionService) {
  // return createTestArticle(ingestionService, SAMPLE_ARTICLES.get(0));
  // }

  // public static ArticleMetadata createTestArticle(IngestionService ingestionService, String
  // doiStub) {
  // ArticleIdentity articleId = ArticleIdentity.create(RhinoTestHelper.prefixed(doiStub));
  // RhinoTestHelper.TestFile sampleFile = new RhinoTestHelper.TestFile(getXmlPath(doiStub));
  // String doi = articleId.getIdentifier();
  //
  // byte[] sampleData;
  // try {
  // sampleData = IOUtils.toByteArray(RhinoTestHelper.alterStream(sampleFile.read(), doi, doi));
  // } catch (IOException e) {
  // throw new RuntimeException(e);
  // }
  //
  // Article reference = readReferenceCase(getJsonPath(doiStub));
  //
  // RhinoTestHelper.TestInputStream input = RhinoTestHelper.TestInputStream.of(sampleData);
  // Archive mockIngestible = createMockIngestible(articleId, input, reference.getAssets());
  // ArticleIngestion ingestion;
  // try {
  // ingestion = ingestionService.ingest(mockIngestible);
  // } catch (IOException e) {
  // throw new RuntimeException(e);
  // }
  // return null; // TODO: Recover ArticleMetadata from ArticleIngestionIdentifier
  // }

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
