package org.ambraproject.rhino.service.impl;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.UUID;

import org.ambraproject.rhino.AbstractRhinoTest;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ingest.ArticleFileInput;
import org.ambraproject.rhino.model.ingest.ArticleItemInput;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.service.ContentRepoPersistenceService;
import org.ambraproject.rhino.util.Archive;
import org.plos.crepo.model.input.RepoObjectInput;
import org.plos.crepo.model.identity.RepoVersion;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Unit tests for {@link ContentRepoPersistenceServiceImpl}.
 */
@ContextConfiguration(classes = ContentRepoPersistenceServiceTest.class)
@Configuration
public class ContentRepoPersistenceServiceTest extends AbstractRhinoTest {

  private static final String ARTICLE_DOI_URI = "info:doi/10.1111/dupp.0000001";

  private static final String ITEM_INPUT_TYPE = "some_input_type";

  private static final Integer NEXT_INGESTION_NUMBER = new Integer(2);

  private static final long FILE_SIZE = 1000L;

  private static final String REPO_KEY = "REPO_KEY";

  private Doi expectedDoi;

  private Article expectedArticle;

  private ArticleIngestion expectedIngestion;

  private String repoUUID;

  /**
   * Initialize test data fixtures.
   */
  @Before
  public void init() {
    repoUUID = UUID.randomUUID().toString();
    expectedDoi = Doi.create(ARTICLE_DOI_URI);

    expectedArticle = new Article();
    expectedArticle.setDoi(expectedDoi.getName());

    expectedIngestion = new ArticleIngestion();
    expectedIngestion.setArticle(expectedArticle);
    expectedIngestion.setIngestionNumber(NEXT_INGESTION_NUMBER);
  }

  @Bean
  public ContentRepoPersistenceService contentRepoPersistenceService() {
    LOG.debug("contentRepoPersistenceService() *");
    final ContentRepoPersistenceService contentRepoPersistenceService =
        spy(ContentRepoPersistenceServiceImpl.class);
    return contentRepoPersistenceService;
  }

  private ArticleFileInput makeArticleFileInput(String name) {
    ManifestXml.ManifestFile manifest = mock(ManifestXml.ManifestFile.class);
    when(manifest.getEntry()).thenReturn(name);
    when(manifest.getCrepoKey()).thenReturn(UUID.randomUUID().toString());
    return ArticleFileInput.builder()
      .setArchive(mock(Archive.class))
      .setManifestFile(manifest)
      .setContentType("application/octet-stream")
      .setDownloadName(name)
      .build();
  }

  /**
   * Test successful creation of an item in content repo.
   */
  @Test
  @DirtiesContext
  public void testCreateArticleItemShouldSucceed() {
    final ImmutableMap<String, ArticleFileInput> expectedFileInputs = ImmutableMap.of(
        "type1", makeArticleFileInput("file1"),
        "type2", makeArticleFileInput("file2"),
        "type3", makeArticleFileInput("file3"),
        "type4", makeArticleFileInput("file4"));
    final int expectedFileCount = expectedFileInputs.size();
    final ImmutableSet<String> expectedFileTypes = expectedFileInputs.keySet();
    final ImmutableSet<String> expectedFileNames = ImmutableSet.of("file1", "file2", "file3",
        "file4");

    final ArticleItemInput expectedItemInput = new ArticleItemInput(
        expectedDoi, expectedFileInputs, ITEM_INPUT_TYPE);

    final ContentRepoService mockContentRepoService =
        buildMockContentRepoService(DESTINATION_BUCKET, REPO_KEY, repoUUID, FILE_SIZE);

    final ContentRepoPersistenceService mockContentRepoPersistenceService =
        applicationContext.getBean(ContentRepoPersistenceService.class);

    final ArticleItem actualArticleItem =
        mockContentRepoPersistenceService.createItem(expectedItemInput, expectedIngestion);

    assertThat(actualArticleItem).isNotNull();
    assertThat(actualArticleItem.getIngestion()).isEqualTo(expectedIngestion);
    assertThat(actualArticleItem.getItemType()).isEqualTo(ITEM_INPUT_TYPE);
    assertThat(actualArticleItem.getDoi()).isEqualTo(expectedDoi.getName());

    final Collection<ArticleFile> actualFiles = actualArticleItem.getFiles();
    assertThat(actualFiles).isNotNull();
    assertThat(actualFiles).hasSize(expectedFileCount);

    actualFiles.forEach(articleFile -> {
      assertThat(articleFile.getBucketName()).isEqualTo(DESTINATION_BUCKET);
      assertThat(articleFile.getIngestion()).isEqualTo(expectedIngestion);
      assertThat(articleFile.getItem()).isEqualTo(actualArticleItem);
      assertThat(articleFile.getCrepoKey()).isEqualTo(REPO_KEY);
      assertThat(articleFile.getCrepoUuid()).isEqualTo(repoUUID);
      assertThat(articleFile.getFileSize()).isEqualTo(FILE_SIZE);
      assertThat(articleFile.getFileType()).isIn(expectedFileTypes);
      assertThat(articleFile.getIngestedFileName()).isIn(expectedFileNames);
    });

    verify(mockContentRepoService, times(expectedFileCount))
        .autoCreateRepoObject(any(RepoObjectInput.class));
    verify(mockContentRepoService, times(expectedFileCount))
        .getRepoObjectMetadata(any(RepoVersion.class));
  }

  /**
   * Test successful adding of ancillary files in content repo.
   */
  @Test
  @DirtiesContext
  public void testPersistAncillaryFilesShouldSucceed() {
    final String destinationBucket = "ancillary";
    final ImmutableList<ArticleFileInput> expectedAncillaryFiles = ImmutableList.of(
        makeArticleFileInput("ancillary1"),
        makeArticleFileInput("ancillary2"),
        makeArticleFileInput("ancillary3"),
        makeArticleFileInput("ancillary4"),
        makeArticleFileInput("ancillary5"),
        makeArticleFileInput("ancillary6"));
    final int expectedFileCount = expectedAncillaryFiles.size();
    final ImmutableSet<String> expectedFileNames = ImmutableSet.of("ancillary1", "ancillary2",
        "ancillary3", "ancillary4", "ancillary5", "ancillary6");

    final ArticlePackage mockArticlePackage = mock(ArticlePackage.class);
    when(mockArticlePackage.getAncillaryFiles()).thenReturn(expectedAncillaryFiles);

    final ContentRepoService mockContentRepoService =
        buildMockContentRepoService(destinationBucket, REPO_KEY, repoUUID, FILE_SIZE);

    final ContentRepoPersistenceService mockContentRepoPersistenceService =
        applicationContext.getBean(ContentRepoPersistenceService.class);

    final Collection<ArticleFile> actualFiles =
        mockContentRepoPersistenceService.persistAncillaryFiles(mockArticlePackage,
            expectedIngestion);

    assertThat(actualFiles).hasSize(expectedFileCount);
    actualFiles.forEach(articleFile -> {
      assertThat(articleFile.getBucketName()).isEqualTo(destinationBucket);
      assertThat(articleFile.getIngestion()).isEqualTo(expectedIngestion);
      assertThat(articleFile.getCrepoKey()).isEqualTo(REPO_KEY);
      assertThat(articleFile.getCrepoUuid()).isEqualTo(repoUUID);
      assertThat(articleFile.getFileSize()).isEqualTo(FILE_SIZE);
      assertThat(articleFile.getIngestedFileName()).isIn(expectedFileNames);
    });

    verify(mockContentRepoService, times(expectedFileCount))
        .autoCreateRepoObject(any(RepoObjectInput.class));
    verify(mockContentRepoService, times(expectedFileCount))
        .getRepoObjectMetadata(any(RepoVersion.class));
  }

  /**
   * Test successful processing for an empty files list.
   */
  @Test
  @DirtiesContext
  public void testEmptyArticleItemFilesShouldSucceed() {
    final ArticleItemInput expectedItemInput = new ArticleItemInput(
        expectedDoi, ImmutableMap.of(), ITEM_INPUT_TYPE);

    final ContentRepoService mockContentRepoService =
        buildMockContentRepoService(DESTINATION_BUCKET);

    final ContentRepoPersistenceService mockContentRepoPersistenceService =
        applicationContext.getBean(ContentRepoPersistenceService.class);

    final ArticleItem actualArticleItem =
        mockContentRepoPersistenceService.createItem(expectedItemInput, expectedIngestion);
    
    assertThat(actualArticleItem).isNotNull();
    assertThat(actualArticleItem.getIngestion()).isEqualTo(expectedIngestion);
    assertThat(actualArticleItem.getItemType()).isEqualTo(ITEM_INPUT_TYPE);
    assertThat(actualArticleItem.getDoi()).isEqualTo(expectedDoi.getName());
    assertThat(actualArticleItem.getFiles()).isEmpty();

    verify(mockContentRepoService, times(0)).autoCreateRepoObject(any(RepoObjectInput.class));
    verify(mockContentRepoService, times(0)).getRepoObjectMetadata(any(RepoVersion.class));
  }

  /**
   * Test successful processing for an empty ancillary files list.
   */
  @Test
  @DirtiesContext
  public void testEmptyAncillaryFilesShouldSucceed() {
    final ArticlePackage mockArticlePackage = mock(ArticlePackage.class);
    when(mockArticlePackage.getAncillaryFiles()).thenReturn(ImmutableList.of());

    final ContentRepoService mockContentRepoService =
        buildMockContentRepoService(DESTINATION_BUCKET);

    final ContentRepoPersistenceService mockContentRepoPersistenceService =
        applicationContext.getBean(ContentRepoPersistenceService.class);

    final Collection<ArticleFile> actualFiles =
        mockContentRepoPersistenceService.persistAncillaryFiles(mockArticlePackage,
            expectedIngestion);
    assertThat(actualFiles).isNotNull();
    assertThat(actualFiles).isEmpty();

    verify(mockContentRepoService, times(0)).autoCreateRepoObject(any(RepoObjectInput.class));
    verify(mockContentRepoService, times(0)).getRepoObjectMetadata(any(RepoVersion.class));
  }
}
