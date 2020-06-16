/*
 * Copyright (c) 2018 Public Library of Science
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobWriteOption;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ingest.ArticleFileInput;
import org.ambraproject.rhino.model.ingest.ArticleItemInput;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.service.ObjectStorageService;
import org.ambraproject.rhino.util.Archive;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Unit tests for {@link GCSObjectStorageServiceImplpl}.
 */
@ContextConfiguration
public class GCSObjectStorageServiceTest extends AbstractJUnit4SpringContextTests {

  @Configuration
  static class ContextConfiguration {
    @Bean
    public Storage storage() throws Exception {
      return mock(Storage.class);
    }

    @Bean
    public ObjectStorageService objectStorageService() {
      return new GCSObjectStorageServiceImpl();
    }

    @Bean
    public RuntimeConfiguration runtimeConfiguration() throws Exception {
      RuntimeConfiguration rc = mock(RuntimeConfiguration.class);
      when(rc.getCorpusBucket()).thenReturn("my-bucket");
      return rc;
    }
  }

  private static final String ARTICLE_DOI_URI = "info:doi/10.1111/dupp.0000001";

  private static final String ITEM_INPUT_TYPE = "some_input_type";

  private static String ARTICLE_DOI = "10.1111/dupp.0000001";

  private static final Integer NEXT_INGESTION_NUMBER = new Integer(2);

  private static final long FILE_SIZE = 1000L;
  private Doi expectedDoi;
  private Article expectedArticle;
  private ArticleIngestion expectedIngestion;

  @Autowired
  private Storage storage;

  @Autowired
  private ObjectStorageService objectStorageService;

  /**
   * Initialize test data fixtures.
   */
  @Before
  public void init() throws IOException {
    Blob result = mock(Blob.class);

    expectedDoi = Doi.create(ARTICLE_DOI_URI);

    expectedArticle = new Article();
    expectedArticle.setDoi(expectedDoi.getName());

    expectedIngestion = new ArticleIngestion();
    expectedIngestion.setArticle(expectedArticle);
    expectedIngestion.setIngestionNumber(NEXT_INGESTION_NUMBER);
    when(storage.get(any(BlobId.class))).thenReturn(result);
    when(result.getSize()).thenReturn(FILE_SIZE);
    WriteChannel writer = mock(WriteChannel.class);
    when(storage.writer(any(BlobInfo.class), Matchers.<BlobWriteOption>anyVararg())).thenReturn(writer);
  }

  private ArticleFileInput makeArticleFileInput(String name) throws IOException {
    ManifestXml.ManifestFile manifest = mock(ManifestXml.ManifestFile.class);
    Archive archive = mock(Archive.class);
    InputStream is = mock(InputStream.class);
    when(is.read(any())).thenReturn(-1);
    when(is.read(any(), anyInt(), anyInt())).thenReturn(-1);
    when(archive.openFile(any(String.class))).thenReturn(is);
    when(manifest.getEntry()).thenReturn(name);
    return ArticleFileInput.builder().setArchive(archive).setManifestFile(manifest)
        .setContentType("application/octet-stream").setDownloadName(name).build();
  }

  /**
   * Test successful creation of an item in object storage.
   * @throws IOException
   */
  @Test
  public void testCreateArticleItemShouldSucceed() throws IOException {
    final ImmutableMap<String, ArticleFileInput> expectedFileInputs = ImmutableMap.of("type1",
        makeArticleFileInput("file1"), "type2", makeArticleFileInput("file2"), "type3", makeArticleFileInput("file3"),
        "type4", makeArticleFileInput("file4"));
    final int expectedFileCount = expectedFileInputs.size();
    final ImmutableSet<String> expectedFileTypes = expectedFileInputs.keySet();
    final ImmutableSet<String> expectedFileNames = ImmutableSet.of("file1", "file2", "file3", "file4");

    final ArticleItemInput expectedItemInput = new ArticleItemInput(expectedDoi, expectedFileInputs, ITEM_INPUT_TYPE);

    final ArticleItem actualArticleItem = objectStorageService.storeItem(expectedItemInput, expectedIngestion);

    assertThat(actualArticleItem).isNotNull();
    assertThat(actualArticleItem.getIngestion()).isEqualTo(expectedIngestion);
    assertThat(actualArticleItem.getItemType()).isEqualTo(ITEM_INPUT_TYPE);
    assertThat(actualArticleItem.getDoi()).isEqualTo(expectedDoi.getName());

    final Collection<ArticleFile> actualFiles = actualArticleItem.getFiles();
    assertThat(actualFiles).isNotNull();
    assertThat(actualFiles).hasSize(expectedFileCount);

    actualFiles.forEach(articleFile -> {
      assertThat(articleFile.getIngestion()).isEqualTo(expectedIngestion);
      assertThat(articleFile.getItem()).isEqualTo(actualArticleItem);
      assertThat(articleFile.getFileType()).isIn(expectedFileTypes);
      assertThat(articleFile.getIngestedFileName()).isIn(expectedFileNames);
    });

    verify(storage, times(expectedFileCount)).writer(any(BlobInfo.class), Matchers.<BlobWriteOption>anyVararg());
  }

  /**
   * Test successful adding of ancillary files in object storage.
   * @throws IOException
   */
  @Test
  @DirtiesContext
  public void testPersistAncillaryFilesShouldSucceed() throws IOException {
    final ImmutableList<ArticleFileInput> expectedAncillaryFiles = ImmutableList.of(makeArticleFileInput("ancillary1"),
        makeArticleFileInput("ancillary2"), makeArticleFileInput("ancillary3"), makeArticleFileInput("ancillary4"),
        makeArticleFileInput("ancillary5"), makeArticleFileInput("ancillary6"));
    final int expectedFileCount = expectedAncillaryFiles.size();
    final ImmutableSet<String> expectedFileNames = ImmutableSet.of("ancillary1", "ancillary2", "ancillary3",
        "ancillary4", "ancillary5", "ancillary6");

    final ArticlePackage mockArticlePackage = mock(ArticlePackage.class);
    when(mockArticlePackage.getAncillaryFiles()).thenReturn(expectedAncillaryFiles);

    final Collection<ArticleFile> actualFiles = objectStorageService.storeAncillaryFiles(mockArticlePackage,
        expectedIngestion);

    assertThat(actualFiles).hasSize(expectedFileCount);
    actualFiles.forEach(articleFile -> {
      assertThat(articleFile.getIngestion()).isEqualTo(expectedIngestion);
      assertThat(articleFile.getFileSize()).isEqualTo(FILE_SIZE);
      assertThat(articleFile.getIngestedFileName()).isIn(expectedFileNames);
    });

    verify(storage, times(expectedFileCount)).writer(any(BlobInfo.class), Matchers.<BlobWriteOption>anyVararg());
  }

  /**
   * Test successful processing for an empty files list.
   */
  @Test
  @DirtiesContext
  public void testEmptyArticleItemFilesShouldSucceed() {
    final ArticleItemInput expectedItemInput = new ArticleItemInput(expectedDoi, ImmutableMap.of(), ITEM_INPUT_TYPE);

    final ArticleItem actualArticleItem = objectStorageService.storeItem(expectedItemInput, expectedIngestion);

    assertThat(actualArticleItem).isNotNull();
    assertThat(actualArticleItem.getIngestion()).isEqualTo(expectedIngestion);
    assertThat(actualArticleItem.getItemType()).isEqualTo(ITEM_INPUT_TYPE);
    assertThat(actualArticleItem.getDoi()).isEqualTo(expectedDoi.getName());
    assertThat(actualArticleItem.getFiles()).isEmpty();
  }

  /**
   * Test successful processing for an empty ancillary files list.
   */
  @Test
  public void testEmptyAncillaryFilesShouldSucceed() {
    final ArticlePackage mockArticlePackage = mock(ArticlePackage.class);
    when(mockArticlePackage.getAncillaryFiles()).thenReturn(ImmutableList.of());

    final Collection<ArticleFile> actualFiles = objectStorageService.storeAncillaryFiles(mockArticlePackage,
        expectedIngestion);
    assertThat(actualFiles).isNotNull();
    assertThat(actualFiles).isEmpty();
  }
}
