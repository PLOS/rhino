/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import org.ambraproject.rhino.AbstractRhinoTest;
import org.ambraproject.rhino.RhinoTestHelper;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.content.xml.CustomMetadataExtractor;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.ManifestXml.Asset;
import org.ambraproject.rhino.content.xml.ManifestXml.AssetTagName;
import org.ambraproject.rhino.content.xml.ManifestXml.ManifestDataException;
import org.ambraproject.rhino.content.xml.ManifestXml.ManifestFile;
import org.ambraproject.rhino.content.xml.ManifestXml.Representation;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ingest.AssetType;
import org.ambraproject.rhino.model.ingest.IngestPackage;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleDatabaseService;
import org.ambraproject.rhino.util.Archive;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.junit.Before;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.fail;

@ContextConfiguration(classes = IngestionServiceTest.class)
@Configuration
public class IngestionServiceTest extends AbstractRhinoTest {

  private static final String MANIFEST_DTD = "manifest.dtd";

  private static final String MANIFEST_XML = "manifest.xml";

  public static final String INGESTED_DOI_URI = "info:doi/10.1111/dupp.0000001";

  public static final ImmutableList<String> ARTICLE_INGEST_ENTRIES = ImmutableList.of(MANIFEST_XML,
      MANIFEST_DTD, "dupp.0000001.s002.jpg", "dupp.0000001.pdf", "dupp.0000001.xml", "manifest.dtd",
      "dupp.0000001.s001.png", "dupp.0000001.s004.docx", "dupp.0000001.s003.xlsx",
      "dupp.0000001.s005.docx", "dupp.0000001.s006.docx", "dupp.0000001.s007.docx");

  private IngestionService ingestionService;

  @Before
  public void init() {
    ingestionService = new IngestionService();
  }

  @Bean
  public CustomMetadataExtractor.Factory customMetadataExtractorFactory() {
    CustomMetadataExtractor.Factory mockMetadataExtractorFactory =
        spy(CustomMetadataExtractor.Factory.class);
    LOG.debug("customMetadataExtractorFactory() * {}", mockMetadataExtractorFactory);
    return mockMetadataExtractorFactory;
  }

  @Bean
  public ArticleCrudService articleCrudService() {
    final ArticleCrudService mockArticleCrudService = mock(ArticleCrudService.class);
    LOG.info("articleCrudService() * --> {}", mockArticleCrudService);
    return mockArticleCrudService;
  }

  @Bean
  public IngestionService ingestionService() {
    final IngestionService mockIngestionService = spy(IngestionService.class);
    LOG.info("ingestionService() * --> {}", mockIngestionService);
    return mockIngestionService;
  }

  public static Archive createStubArchive(byte[] manifestXml, Collection<String> entryNames) {
    Map<String, ByteSource> fileMap = new HashMap<>();

    for (String entryName : entryNames) {
      fileMap.put(entryName, ByteSource.empty());
    }

    if (manifestXml != null) {
      fileMap.put(MANIFEST_XML, ByteSource.wrap(manifestXml));
    }

    return Archive.pack("test", fileMap);
  }

  private static ArticleItem createStubArticleItem() {
    ArticleItem articleItem = new ArticleItem();
    ArticleIngestion articleIngestion = new ArticleIngestion();
    Article article = new Article();
    article.setDoi("test");
    articleItem.setIngestion(articleIngestion);
    articleIngestion.setArticle(article);
    return articleItem;
  }

  private static Collection<String> getBaseEntryNames() {
    Collection<String> entryNames = new ArrayList<>(8);
    entryNames.add(MANIFEST_XML);
    entryNames.add("pone.0000001.xml");
    entryNames.add("pone.0000001.g001.PNG");
    entryNames.add("pone.0000001.g001.tif");
    entryNames.add("pone.0000001.g001.PNG_I");
    entryNames.add("pone.0000001.g001.PNG_L");
    entryNames.add("pone.0000001.g001.PNG_M");
    entryNames.add("pone.0000001.g001.PNG_S");
    return entryNames;
  }

  @Test(expected = RestClientException.class)
  public void testGetManifestXml() throws Exception {
    Archive invalidTestArchive = createStubArchive(new byte[] {}, getBaseEntryNames());
    ingestionService.getManifestXml(invalidTestArchive);
  }

  @Test(expected = RestClientException.class)
  public void testGetManifestXml_missingManifest() throws Exception {
    Collection<String> entryNames = getBaseEntryNames();
    entryNames.remove(MANIFEST_XML);
    Archive invalidTestArchive = createStubArchive(null, entryNames);
    ingestionService.getManifestXml(invalidTestArchive);
  }

  @Test
  public void testValidateManuscript() {
    ingestionService.validateManuscript(Doi.create("test"), "test");
  }

  @Test(expected = RestClientException.class)
  public void testValidateManuscript_invalid() {
    ingestionService.validateManuscript(Doi.create("test"), "test2");
  }

  @Test
  public void testValidateAssetUniqueness() {
    ingestionService.validateAssetUniqueness(Doi.create("test"),
        ImmutableList.of(createStubArticleItem()));
  }

  @Test(expected = RestClientException.class)
  public void testValidateAssetUniqueness_invalid() {
    ingestionService.validateAssetUniqueness(Doi.create("test2"),
        ImmutableList.of(createStubArticleItem()));
  }

  /**
   * Test successful processing of <b>manifest.xml</b> file.
   *
   * @param entryNames The list of entry names.
   *
   * @throws IOException if manifest file cannot be processed
   */
  @Test
  public void testGetManifestXmlShouldSucceed() throws IOException {
    Collection<String> entryNames = getBaseEntryNames();
    final URL xmlDataResource = Resources.getResource(IngestionServiceTest.class, MANIFEST_XML);
    final File manifestFile = FileUtils.toFile(xmlDataResource);
    final byte[] manifestData = FileUtils.readFileToByteArray(manifestFile);
    final Archive testArchive = createStubArchive(manifestData, entryNames);

    final ManifestXml actualManifest = ingestionService.getManifestXml(testArchive);

    final Asset articleAsset = actualManifest.getArticleAsset();
    assertThat(articleAsset.getRepresentations()).hasSize(2);

    final Optional<Representation> printable = articleAsset.getRepresentation("printable");
    assertThat(printable.isPresent()).isTrue();
    final Representation printablePresentation = printable.get();
    assertThat(printablePresentation.getType()).isEqualTo("printable");
    final ManifestFile presentationManifest = printablePresentation.getFile();
    assertThat(presentationManifest.getEntry()).isEqualTo("dupp.0000001.pdf");
    assertThat(presentationManifest.getCrepoKey()).isEqualTo("10.1111/dupp.0000001.pdf");

    final ImmutableList<Asset> assets = actualManifest.getAssets();
    assertThat(assets).hasSize(8);
    final MutableInt supplementary_count = new MutableInt();
    assets.forEach(asset -> {
      final AssetTagName assetTag = asset.getAssetTagName();
      final AssetType assetType = asset.getType();
      if (assetTag.equals(ManifestXml.AssetTagName.ARTICLE)) {
        assertThat(assetType).isEqualTo(AssetType.ARTICLE);
        assertThat(asset).isEqualTo(articleAsset);
      } else if (assetTag.equals(ManifestXml.AssetTagName.OBJECT)) {
        assertThat(asset.getRepresentations()).hasSize(1);
        assertThat(assetType).isEqualTo(AssetType.SUPPLEMENTARY_MATERIAL);

        supplementary_count.increment();
        final String expectedUri =
            String.format("info:doi/10.1111/dupp.0000001.s%03d", supplementary_count.intValue());
        assertThat(asset.getUri()).isEqualTo(expectedUri);

        if (supplementary_count.intValue() == 1) {
          // First asset will have "strkImage" set to True.
          assertThat(asset.isStrikingImage()).isTrue();
        } else {
          assertThat(asset.isStrikingImage()).isFalse();
        }
      } else {
        assert false : String.format("Unexpected test case for asset tag: '%s'", assetTag);
      }
    });

    final ImmutableList<String> expectedEntries = ImmutableList.of(MANIFEST_XML, MANIFEST_DTD);
    final ImmutableList<ManifestFile> ancillaryFiles = actualManifest.getAncillaryFiles();
    assertThat(ancillaryFiles).hasSize(2);
    ancillaryFiles.forEach(ancillary -> {
      final String entry = ancillary.getEntry();
      assertThat(expectedEntries).contains(entry);
      if (entry.equals(MANIFEST_XML)) {
        assertThat(ancillary.getCrepoKey()).isEqualTo("10.1111/dupp.0000001.manifest.xml");
        assertThat(ancillary.getMimetype()).isEqualTo("application/xml");
      } else if (entry.equals(MANIFEST_DTD)) {
        assertThat(ancillary.getCrepoKey()).isEqualTo("10.1111/dupp.0000001.manifest.dtd");
        assertThat(ancillary.getMimetype()).isEqualTo("application/xml-dtd");
      } else {
        assert false : String.format("Unexpected test case for entry: '%s'", entry);
      }
    });
  }

  /**
   * Test exception handling with an <b>empty</b> manifest.xml file.
   *
   * @param entryNames The list of entries in the test archive.
   *
   * @throws IOException if manifest file cannot be processed
   */
  @Test
  public void testEmptyManifestXmlShouldFail() throws IOException {
    Collection<String> entryNames = getBaseEntryNames();
    final URL xmlDataResource =
        Resources.getResource(IngestionServiceTest.class, "empty_manifest.xml");
    final File manifestFile = FileUtils.toFile(xmlDataResource);
    final byte[] manifestData = FileUtils.readFileToByteArray(manifestFile);
    final Archive testArchive = createStubArchive(manifestData, entryNames);

    final ManifestXml actualManifest = ingestionService.getManifestXml(testArchive);

    assertThat(actualManifest).isNotNull();

    try {
      actualManifest.getArticleAsset();
      fail("Expecting exception, but nothing was thrown.");
    } catch (Exception exception) {
      assertThat(exception).isInstanceOf(ManifestDataException.class);
      assertThat(exception).hasMessageThat().isEqualTo("'article' node must have 'uri' attribute");
    }
  }

  /**
   * Test exception handling with an <b>duplicate URI</b> in manifest.xml file.
   *
   * @param entryNames The list of entries in the test archive.
   *
   * @throws IOException if manifest file cannot be processed
   */
  @Test
  public void testDuplicateUriManifestXmlShouldFail()
      throws IOException {
    Collection<String> entryNames = getBaseEntryNames();
    final URL xmlDataResource =
        Resources.getResource(IngestionServiceTest.class, "duplicate_assets_manifest.xml");
    final File manifestFile = FileUtils.toFile(xmlDataResource);
    final byte[] manifestData = FileUtils.readFileToByteArray(manifestFile);
    final Archive testArchive = createStubArchive(manifestData, entryNames);

    final ManifestXml actualManifest = ingestionService.getManifestXml(testArchive);

    assertThat(actualManifest).isNotNull();

    try {
      actualManifest.getArticleAsset();
      fail("Expecting exception, but nothing was thrown.");
    } catch (Exception exception) {
      assertThat(exception).isInstanceOf(ManifestDataException.class);
      assertThat(exception).hasMessageThat()
          .isEqualTo("Manifest has assets with duplicate uri: info:doi/10.1111/dupp.0000001.s001");
    }
  }

  /**
   * Test successful article ingestion.
   * 
   * @throws IOException if archive file cannot be processed
   * @throws SAXException if any XML parsing errors
   * @throws ParserConfigurationException if a DocumentBuilder cannot be created
   */
  @Test
  @DirtiesContext()
  public void testArticleIngestShouldSucceed()
      throws IOException, ParserConfigurationException, SAXException {
    final URL xmlDataResource = Resources.getResource(IngestionServiceTest.class, MANIFEST_XML);
    final File manifestFile = FileUtils.toFile(xmlDataResource);
    final byte[] manifestData = FileUtils.readFileToByteArray(manifestFile);
    final Archive testArchive = createStubArchive(manifestData, ARTICLE_INGEST_ENTRIES);

    final String manuscriptEntry = "dupp.0000001.xml";
    final URL manuscriptResource =
        Resources.getResource(IngestionServiceTest.class, manuscriptEntry);
    final Document manuscript = RhinoTestHelper.loadXMLFromString(manuscriptResource);

    final Article expectedArticle = new Article();
    expectedArticle.setDoi("10.1111/dupp.0000001");

    final ArticleIngestion expectedIngestion = new ArticleIngestion();
    expectedIngestion.setArticle(expectedArticle);
    expectedIngestion.setIngestionNumber(1);

    final Doi expectedArticleDoi = Doi.create(INGESTED_DOI_URI);

    final ArticleDatabaseService mockArticleDatabaseService =
        applicationContext.getBean(ArticleDatabaseService.class);
    when(mockArticleDatabaseService.persistArticle(expectedArticleDoi)).thenReturn(expectedArticle);
    when(mockArticleDatabaseService.persistIngestion(any(Article.class), any(IngestPackage.class)))
        .thenReturn(expectedIngestion);

    final IngestionService mockIngestionService =
        applicationContext.getBean(IngestionService.class);
    doReturn(manuscript).when(mockIngestionService).getDocument(testArchive, manuscriptEntry);

    final ArticleIngestion actualIngestion = mockIngestionService.ingest(testArchive);

    assertThat(actualIngestion).isNotNull();
    assertThat(actualIngestion).isEqualTo(expectedIngestion);
    assertThat(actualIngestion.getArticle()).isEqualTo(expectedArticle);

    final CustomMetadataExtractor.Factory mockMetadataExtractorFactory =
        applicationContext.getBean(CustomMetadataExtractor.Factory.class);
    final ArticleCrudService mockArticleCrudService =
        applicationContext.getBean(ArticleCrudService.class);

    verify(mockIngestionService).getDocument(testArchive, manuscriptEntry);
    verify(mockMetadataExtractorFactory).parse(manuscript);

    final ImmutableList<String> expectedDois =
        ImmutableList.of(INGESTED_DOI_URI, "info:doi/10.1111/dupp.0000001.s001",
            "info:doi/10.1111/dupp.0000001.s002", "info:doi/10.1111/dupp.0000001.s003",
            "info:doi/10.1111/dupp.0000001.s004", "info:doi/10.1111/dupp.0000001.s005",
            "info:doi/10.1111/dupp.0000001.s006", "info:doi/10.1111/dupp.0000001.s007");
    final int callCount = mockingDetails(mockArticleCrudService).getInvocations().size();
    assertThat(callCount).isEqualTo(expectedDois.size());
    expectedDois.forEach(doiUri -> {
      final Doi assetDoi = Doi.create(doiUri);
      verify(mockArticleCrudService).getAllArticleItems(assetDoi);
    });

    verify(mockArticleDatabaseService).persistArticle(expectedArticleDoi);
    verify(mockArticleDatabaseService).persistIngestion(any(Article.class), any(IngestPackage.class));
  }
}
