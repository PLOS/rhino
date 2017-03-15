package org.ambraproject.rhino.service.impl;

import com.google.gson.Gson;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.filestore.FileStoreService;
import org.ambraproject.filestore.impl.FileSystemImpl;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.WriteResult;
import org.ambraproject.rhino.service.WriteResult.Action;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleVisibility;
import org.ambraproject.rhino.view.asset.groomed.GroomedImageView;
import org.ambraproject.rhino.view.asset.raw.RawAssetFileCollectionView;
import org.ambraproject.rhino.view.asset.raw.RawAssetFileView;
import org.ambraproject.service.article.ArticleClassifier;
import org.ambraproject.service.article.ArticleService;
import org.ambraproject.service.article.ArticleServiceImpl;
import org.ambraproject.service.article.DummyArticleClassifier;
import org.ambraproject.service.syndication.SyndicationService;
import org.ambraproject.service.syndication.impl.SyndicationServiceImpl;
import org.apache.commons.io.IOUtils;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;

/**
 * This suite tests the HAPPY PATHS ONLY for the AssetCrudService.
 *
 * Created by jkrzemien on 7/23/14.
 *
 * NOTE: THIS IS AN INTEGRATION TEST (AS DEFINED IN SPRING DOC), NOT A UNIT TEST.
 *
 * IF YOU MANGLE THE DATA SOURCES/PROVIDERS YOU COULD BE RE-WRITING A REAL ENVIRONMENT'S DATA
 */

@ContextConfiguration
public class AssetCrudServiceIntegrationTest extends AbstractTransactionalJUnit4SpringContextTests {

    private static final String ID = "10.1371/journal.pbio.0020365";
    private static final String DOI = "info:doi/" + ID;
    private static final String EXTENSION = "xxx";
    private static final String NEW_EXTENSION = "zzz";
    private static final String FILE_CONTENT = "DaFile Contents";
    public static final String INT_TEST_OBJECT_STORE = "/tmp/intTestObjectStore";

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private HibernateTemplate preconditions;

    @Autowired
    private AssetCrudService serviceUnderTest;

    private ArticleAsset createAsset() {
        ArticleAsset asset = new ArticleAsset();
        asset.setExtension(EXTENSION);
        asset.setDoi(DOI);
        return asset;
    }

    private Journal createJournal(String key) {
        Journal journal = new Journal(key);
        journal.setID(1L);
        return journal;
    }

    private Article createArticle() {
        List<ArticleAsset> assets = new ArrayList<ArticleAsset>();
        assets.add(createAsset());

        Article article = new Article();
        article.setDoi(DOI);
        article.setAssets(assets);
        article.setState(Article.STATE_UNPUBLISHED);
        return article;
    }

    private Article createArticleWithJournals() {

        Journal journal = createJournal("pone");
        preconditions.save(journal);

        Set<Journal> journals = new HashSet<Journal>();
        journals.add(journal);

        Article article = createArticle();
        article.setJournals(journals);
        return article;
    }

    @Test
    public void uploadTest() throws FileStoreException, IOException {
        /**
         * Test preconditions, an article with an asset associated to it must exist in DB
         * This could be done also using DBUnit...or by running a test-data.sql previously
         */
        Article article = createArticle();
        preconditions.save(article);

        /**
         * Set up API under test parameters
         */
        InputStream file = new ByteArrayInputStream(FILE_CONTENT.getBytes());
        AssetFileIdentity assetId = AssetFileIdentity.create(DOI, NEW_EXTENSION);

        Date testDate = new Date();

        /**
         * Invoke API
         */
        WriteResult<ArticleAsset> response = serviceUnderTest.upload(file, assetId);

        /**
         * Perform validations :P
         */
        assertNotNull(response);
        assertEquals(Action.CREATED, response.getAction());
        assertEquals(HttpStatus.CREATED, response.getStatus());
        ArticleAsset writtenAsset = response.getWrittenObject();
        assertEquals(DOI, writtenAsset.getDoi());
        Long newIDShouldBe = article.getAssets().get(0).getID() + 1;
        assertEquals(newIDShouldBe, writtenAsset.getID());
        assertEquals(NEW_EXTENSION, writtenAsset.getExtension());
        assertEquals("application/octet-stream", writtenAsset.getContentType());
        assertEquals(FILE_CONTENT.length(), writtenAsset.getSize());
        assertTrue(writtenAsset.getCreated().after(testDate));
        assertEquals(writtenAsset.getCreated(), writtenAsset.getLastModified());
    }

    @Test
    public void overwriteTest() throws IOException, FileStoreException {
        /**
         * Test preconditions: an asset must exist in DB
         */
        preconditions.save(createAsset());

        /**
         * Set up API under test parameters
         */
        String NEW_CONTENT = "NEW STUFF";
        InputStream file = new ByteArrayInputStream(NEW_CONTENT.getBytes());
        AssetFileIdentity assetId = AssetFileIdentity.create(DOI, EXTENSION);

        /**
         * Invoke API
         */
        serviceUnderTest.overwrite(file, assetId);

        /**
         * Perform validations :P
         */
        InputStream response = serviceUnderTest.read(assetId);

        /**
         * Perform validations :P
         */
        assertNotNull(response);
        String content = IOUtils.toString(response);
        assertNotNull(content);
        assertEquals(NEW_CONTENT, content);
    }

    @Test
    public void readTest() throws IOException, FileStoreException {
        /**
         * Test preconditions: an asset must exist in DB
         */
        ArticleAsset asset = createAsset();
        preconditions.save(asset);

        AssetFileIdentity assetId = AssetFileIdentity.create(asset.getDoi(), EXTENSION);
        InputStream file = new ByteArrayInputStream(FILE_CONTENT.getBytes());

        serviceUnderTest.overwrite(file, assetId);

        /**
         * Invoke API
         */
        InputStream response = serviceUnderTest.read(assetId);

        /**
         * Perform validations :P
         */
        assertNotNull(response);
        String content = IOUtils.toString(response);
        assertNotNull(content);
        assertEquals(FILE_CONTENT, content);
    }

    @Test
    public void reproxyTest() throws IOException, FileStoreException {
        /**
         * Test preconditions: an asset must exist in DB
         */
        preconditions.save(createAsset());

        /**
         * Set up API under test parameters
         */
        AssetFileIdentity assetId = AssetFileIdentity.create(DOI, EXTENSION);

        /**
         * Invoke API
         */
        List<URL> urlList = serviceUnderTest.reproxy(assetId);

        /**
         * Perform validations :P
         */
        assertNotNull(urlList);
        for (URL url : urlList) {
            assertNotNull(url);
            assertTrue(new File(url.getPath()).exists());
        }
    }

    @Test
    public void readFileMetadataTest() throws IOException, FileStoreException {
        /**
         * Test preconditions: an asset must exist in DB
         */
        Article article = createArticleWithJournals();
        preconditions.save(article);

        /**
         * Set up API under test parameters
         */
        AssetFileIdentity assetId = AssetFileIdentity.create(DOI, EXTENSION);

        /**
         * Invoke API
         */
        Transceiver transceiver = serviceUnderTest.readFileMetadata(assetId);


        /**
         * Perform validations :P
         */
        assertNotNull(transceiver);

        ArticleVisibility articleVisibility = ArticleVisibility.create(article);
        RawAssetFileView rafv = new RawAssetFileView(article.getAssets().get(0), articleVisibility);

        Gson gson = new Gson();
        assertEquals(gson.toJson(rafv), transceiver.readJson(gson));
    }

    @Test
    public void readMetadataTest() throws IOException, FileStoreException {
        /**
         * Test preconditions: an asset must exist in DB
         */
        Article article = createArticleWithJournals();
        preconditions.save(article);

        /**
         * Set up API under test parameters
         */
        AssetIdentity assetId = AssetIdentity.create(DOI);

        /**
         * Invoke API
         */
        Transceiver transceiver = serviceUnderTest.readMetadata(assetId);


        /**
         * Perform validations :P
         */
        assertNotNull(transceiver);

        ArticleVisibility articleVisibility = ArticleVisibility.create(article);
        RawAssetFileCollectionView rafcv = new RawAssetFileCollectionView(article.getAssets(), articleVisibility);

        Gson gson = new Gson();
        assertEquals(gson.toJson(rafcv), transceiver.readJson(gson));
    }

    @Test
    public void readFigureMetadataTest() throws IOException, FileStoreException {
        /**
         * Test preconditions: an asset must exist in DB
         */
        Article article = createArticleWithJournals();
        article.getAssets().get(0).setExtension("TIF");
        ArticleAsset asset = createAsset();
        asset.setExtension("PNG");
        article.getAssets().add(asset);
        preconditions.save(article);

        /**
         * Set up API under test parameters
         */
        AssetIdentity assetId = AssetIdentity.create(DOI);

        /**
         * Invoke API
         */
        Transceiver transceiver = serviceUnderTest.readFigureMetadata(assetId);


        /**
         * Perform validations :P
         */
        assertNotNull(transceiver);

        ArticleVisibility articleVisibility = ArticleVisibility.create(article);
        GroomedImageView giv = GroomedImageView.create(article.getAssets());

        Gson gson = new Gson();
        assertEquals(gson.toJson(giv), transceiver.readJson(gson));
    }

    @Test
    public void deleteTest() throws IOException, FileStoreException {
        /**
         * Test preconditions: an asset must exist in DB
         */
        Article article = createArticle();
        preconditions.save(article);

        /**
         * Set up API under test parameters
         */
        AssetFileIdentity assetId = AssetFileIdentity.create(DOI, EXTENSION);

        /**
         * Invoke API
         */
        serviceUnderTest.delete(assetId);


        /**
         * Perform validations
         *
         * Attempt to read the recently deleted asset would incur in an exception that will tell us that delete
         * operation worked fine.
         *
         * BUT:
         *
         * Assets is defined as: <list name="assets" cascade="all-delete-orphan"> in the HBM file.
         *
         * When you delete an object from Hibernate you have to delete it from *all* collections it exists in...
         * If we don't remove the deleted assets from our POJO, they'll keep refering to the deleted data
         * on the database, and Hibernate will complain.
         *
         * Which causes:
         * org.springframework.dao.InvalidDataAccessApiUsageException: deleted object would be re-saved by cascade
         * (remove deleted object from associations): [org.ambraproject.models.ArticleAsset#1]
         */
        article.getAssets().clear();

        thrown.expect(RestClientException.class);
        thrown.expectMessage(String.format("Item not found at the provided ID: %s.%s", ID, EXTENSION));

        serviceUnderTest.read(assetId);
    }


    @Configuration
    static class TestConfig extends HibernateConfig {

        @Bean
        public HibernateTemplate getHibernateTemplateDependency(SessionFactory sessionFactory) {
            return new HibernateTemplate(sessionFactory);
        }

        @Bean
        public ArticleService getArticleServiceDependency(SessionFactory sessionFactory) {
            ArticleServiceImpl articleService = new ArticleServiceImpl();
            articleService.setSessionFactory(sessionFactory);
            return articleService;
        }

        @Bean
        public FileStoreService getFileStoreServiceDependency() throws IOException {
            File fsRepo = new File(INT_TEST_OBJECT_STORE);
            if (fsRepo.exists()) {
                fsRepo.delete();
            }
            fsRepo.mkdir();
            return new FileSystemImpl(fsRepo, "");
        }

        @Bean
        public ArticleClassifier getArticleClassifierDependency() {
            return new DummyArticleClassifier();
        }

        @Bean
        public SyndicationService getSyndicationServiceDependency() {
            return new SyndicationServiceImpl();
        }

        @Bean
        public Gson getGsonDependency() {
            return new Gson();
        }

        @Bean
        public AssetCrudService getServiceUnderTest() {
            return new AssetCrudServiceImpl();
        }
    }

}
