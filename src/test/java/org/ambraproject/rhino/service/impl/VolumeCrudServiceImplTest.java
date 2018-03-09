package org.ambraproject.rhino.service.impl;

import com.google.gson.Gson;
import org.ambraproject.rhino.identity.VolumeIdentifier;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.view.journal.VolumeInputView;
import org.ambraproject.rhino.view.journal.VolumeOutputView;
import org.hibernate.Query;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = VolumeCrudServiceImplTest.class)
@Configuration
public class VolumeCrudServiceImplTest extends AbstractStubbingArticleTest {

  private VolumeCrudService mockVolumeCrudService;

  private JournalCrudService mockJournalCrudService;

  private VolumeOutputView.Factory mockVolumeOutputViewFactory;

  private Query mockQuery;

  private HibernateTemplate mockHibernateTemplate;

  private Volume stubVolume = createStubVolume();

  private Journal stubJournal = createStubJournal();

  private final String VOLUME_CREATION_JSON = "{\"doi\": \"10.1371/volume.pmed.v01\", \"displayName\": \"2004\"}";

  private VolumeInputView stubVolumeInputView;

  @Bean
  public VolumeCrudService volumeCrudService() {
    mockVolumeCrudService = spy(VolumeCrudServiceImpl.class);
    LOG.debug("volumeCrudService() * --> {}", mockVolumeCrudService);
    return mockVolumeCrudService;
  }

  @Bean
  public VolumeOutputView.Factory volumeOutputViewFactory() {
    mockVolumeOutputViewFactory = mock(VolumeOutputView.Factory.class);
    LOG.debug("volumeOutputViewFactory() * --> {}", mockVolumeOutputViewFactory);
    return mockVolumeOutputViewFactory;
  }

  private Volume createStubVolume() {
    final Volume stubVolume = new Volume("test");
    stubVolume.setIssues(new ArrayList<>());
    stubVolume.setLastModified(new Date());
    return stubVolume;
  }

  private Journal createStubJournal() {
    final Journal stubJournal = new Journal("test");
    stubJournal.setVolumes(new ArrayList<>());
    return stubJournal;
  }

  /**
   * Returns a mock query used to mock a HibernateTemplate.
   */
  private Answer<Query> createQueryAnswer() {
    return invocation -> {
      final String sql = invocation.getArgument(0);

      // Return appropriate data based on SQL.
      LOG.info("sql: {}", sql);
      if (sql.contains("FROM Volume")) {
        when(mockQuery.uniqueResult()).thenReturn(stubVolume);
      } else if (sql.contains("FROM Journal")) {
        when(mockQuery.uniqueResult()).thenReturn(stubJournal);
      }
      return mockQuery;
    };
  }

  /**
   * Returns a mock query used to mock a HibernateTemplate that returns no values.
   */
  private Answer<Query> createEmptyQueryAnswer() {
    return invocation -> {
      when(mockQuery.uniqueResult()).thenReturn(null);
      return mockQuery;
    };
  }

  public VolumeCrudServiceImplTest() {
    super(true);
  }

  @BeforeMethod
  public void initMocks() throws IllegalAccessException, NoSuchFieldException {
    mockVolumeCrudService = applicationContext.getBean(VolumeCrudService.class);
    mockJournalCrudService = applicationContext.getBean(JournalCrudService.class);
    mockVolumeOutputViewFactory = applicationContext.getBean(VolumeOutputView.Factory.class);
    mockQuery = mock(Query.class);
    mockHibernateTemplate = buildMockHibernateTemplate(createQueryAnswer());
    Gson mockEntityGson = applicationContext.getBean(Gson.class);
    stubVolumeInputView = mockEntityGson.fromJson(VOLUME_CREATION_JSON, VolumeInputView.class);
  }

  @Test
  public void testReadVolume() throws Exception {

    mockVolumeCrudService.readVolume(VolumeIdentifier.create("test"));

    verify(mockHibernateTemplate, times(5)).execute(any());
  }

  @Test
  public void testReadVolumeByIssue() throws Exception {
    mockVolumeCrudService.readVolumeByIssue(new Issue("test"));

    verify(mockHibernateTemplate, times(6)).execute(any());
  }

  @Test
  public void testCreate() throws Exception {
    when(mockJournalCrudService.readJournal("test")).thenReturn(stubJournal);

    HibernateTemplate mockHibernateTemplate = buildMockHibernateTemplate(createEmptyQueryAnswer());

    mockVolumeCrudService.create("test", stubVolumeInputView);

    verify(mockHibernateTemplate).save(any(Journal.class));
  }

  @Test(expectedExceptions = RestClientException.class)
  public void testCreate_volumeExists() throws Exception {
    mockVolumeCrudService.create("test", stubVolumeInputView);
  }

  @Test
  public void testUpdate() throws Exception {
    mockVolumeCrudService.update(VolumeIdentifier.create("test"), stubVolumeInputView);

    verify(mockHibernateTemplate).update(any(Volume.class));
  }

  @Test
  public void testServeVolume() throws Exception {
    when(mockJournalCrudService.readJournalByVolume(any())).thenReturn(stubJournal);

    mockVolumeCrudService.serveVolume(VolumeIdentifier.create("test"));

    verify(mockHibernateTemplate, times(7)).execute(any());
  }

  @Test
  public void testDelete() throws Exception {
    mockVolumeCrudService.delete(VolumeIdentifier.create("test"));

    verify(mockHibernateTemplate).delete(any(Volume.class));
  }

  @Test
  public void testGetJournalOf() throws Exception {
    mockVolumeCrudService.getJournalOf(stubVolume);

    verify(mockHibernateTemplate, times(4)).execute(any());
  }

}