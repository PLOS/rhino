package org.ambraproject.rhino.service.impl;

import static org.ambraproject.rhino.identity.AssetFileIdentity.create;
import static org.ambraproject.rhino.util.TestReflectionUtils.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.filestore.FileStoreService;
import org.ambraproject.filestore.impl.FSIDMapper;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.WriteResult;
import org.ambraproject.rhino.service.WriteResult.Action;
import org.ambraproject.service.article.ArticleClassifier;
import org.ambraproject.service.article.ArticleService;
import org.ambraproject.service.syndication.SyndicationService;
import org.hibernate.criterion.DetachedCriteria;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.google.gson.Gson;

/**
 * Created by jkrzemien on 7/15/14.
 */

@RunWith(MockitoJUnitRunner.class)
public class AssetCrudServiceImplTest {

	private static final String DOI = "info:doi/10.1371/journal.pbio.0020365";
	private static final String EXTENSION = "new_ext";
	private static final String FILE_CONTENT = "DaFile Contents";

    @Rule public ExpectedException expectedEx = ExpectedException.none();

	@Mock private HibernateTemplate hibernateTemplate;
	@Mock private FileStoreService fileStoreService;
	@Mock private ArticleClassifier articleClassifier;
	@Mock private ArticleService articleService;
	@Mock private SyndicationService syndicationService;

	@InjectMocks
	private AssetCrudService serviceUnderTest = new AssetCrudServiceImpl();

	public AssetCrudServiceImplTest() {
		setField(serviceUnderTest, "entityGson", new Gson());
	}
	
	@Before
	public void setUp() {
		reset(hibernateTemplate, fileStoreService, articleClassifier, articleService, syndicationService);
	}
	
	@Test
	public void uploadTest() throws FileStoreException, IOException {
		/**
		 * Pre build some object for later usage in mock responses
		 */
		List<ArticleAsset> listOfExistingAssets = new ArrayList<ArticleAsset>();
		listOfExistingAssets.add(new ArticleAsset(DOI, "old_ext"));
		OutputStream outputStream = new ByteArrayOutputStream();
		ArrayList<Object[]> hibernateExistingId = new ArrayList<Object[]>();
		hibernateExistingId.add(new Object[] { new BigInteger("1"), 1 });

		/**
		 * Set up expectations for mock objects
		 */
		when(hibernateTemplate.findByCriteria(any(DetachedCriteria.class)))
				.thenReturn(listOfExistingAssets);
		when(fileStoreService.objectIDMapper())
				.thenReturn(new FSIDMapper());
		when(fileStoreService.getFileOutStream(anyString(), anyLong()))
				.thenReturn(outputStream);
		when(hibernateTemplate.execute(any(HibernateCallback.class)))
				.thenReturn(hibernateExistingId);
		when(hibernateTemplate.save(any(ArticleAsset.class))).thenAnswer(new Answer<ArticleAsset>() {
            @Override
            public ArticleAsset answer(InvocationOnMock invocation) throws Throwable {
            	ArticleAsset asset = (ArticleAsset) invocation.getArguments()[0];
                asset.setID(1L); // ArticleAsset mangling here so it won't fail Preconditions.checkNotNull(asset.getID());
                /**
                 *  Returning null since Hibernate's save() returned value is not being used in Rhino's code 
                 *  and also because it is required to implement Serializable, which Ambra entities are not.
                 *  A.K.A. I don't think this will change in the near future ;)
                 */
                return null;
            }
        });

		/**
		 * Set up API under test parameters
		 */
		InputStream file = new ByteArrayInputStream(FILE_CONTENT.getBytes());
		AssetFileIdentity assetId = create(DOI, EXTENSION);
		
		Date testDate = new Date();
		
		/**
		 * Invoke API
		 */
		WriteResult<ArticleAsset> response = serviceUnderTest.upload(file, assetId);
		
		/**
		 * Perform validations :P
		 */
		assertNotNull(response);
		assertEquals(response.getAction(), Action.CREATED);
		assertEquals(response.getStatus(), HttpStatus.CREATED);
		ArticleAsset writtenAsset = response.getWrittenObject();
		assertEquals(writtenAsset.getDoi(), DOI);
		assertEquals(writtenAsset.getID(), (Long) 1L);
		assertEquals(writtenAsset.getExtension(), EXTENSION);
		assertEquals(writtenAsset.getContentType(), "application/octet-stream");
		assertEquals(writtenAsset.getSize(), FILE_CONTENT.length());
		assertTrue(writtenAsset.getCreated().after(testDate));
		assertEquals(writtenAsset.getCreated(), writtenAsset.getLastModified());
		
		InOrder inOrder = inOrder(hibernateTemplate, fileStoreService);
		inOrder.verify(hibernateTemplate).findByCriteria(any(DetachedCriteria.class));
		inOrder.verify(fileStoreService).objectIDMapper();
		inOrder.verify(fileStoreService).getFileOutStream(anyString(), anyLong());
		inOrder.verify(hibernateTemplate).execute(any(HibernateCallback.class));
		inOrder.verify(hibernateTemplate).save(any(ArticleAsset.class));
		inOrder.verify(hibernateTemplate).execute(any(HibernateCallback.class));
		
		verifyNoMoreInteractions(fileStoreService, hibernateTemplate);
		verifyZeroInteractions(articleClassifier, articleService, syndicationService);
	}

}
