package org.ambraproject.rhino.service.impl;


import edu.emory.mathcs.backport.java.util.Collections;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleList;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.view.article.ArticleRevisionView;
import org.ambraproject.rhino.view.journal.ArticleListView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.fail;


@ContextConfiguration(classes = ArticleListCrudServiceImplTest.class)
@Configuration
public class ArticleListCrudServiceImplTest extends AbstractStubbingArticleTest {
  protected static final String ARTICLE_LIST_ID_TYPE = "valid-type";
  protected static final String ARTICLE_LIST_ID_JOURNAL = "valid-journal";
  protected static final String ARTICLE_LIST_ID_KEY = "valid-key";
  protected static final String ARTICLE_LIST_TITLE = "article list 1";

  private ArticleListCrudService mockArticleListCrudService;

  private ArticleCrudService mockArticleCrudService;

  private HibernateTemplate mockHibernateTemplate;

  @BeforeMethod
  public void initMocks() throws IllegalAccessException, NoSuchFieldException {
    mockArticleListCrudService = applicationContext.getBean(ArticleListCrudService.class);
    mockHibernateTemplate = applicationContext.getBean(HibernateTemplate.class);
  }

  @Bean
  public ArticleListCrudService articleListCrudService() {
    mockArticleListCrudService = spy(ArticleListCrudServiceImpl.class);
    LOG.debug("articleListCrudService() * --> {}", mockArticleListCrudService);
    return mockArticleListCrudService;
  }

  @Bean
  public ArticleCrudService articleCrudService() {
    mockArticleCrudService = spy(ArticleCrudServiceImpl.class);
    LOG.debug("articleCrudService() * --> {}", mockArticleCrudService);
    return mockArticleCrudService;
  }

  @Bean
  public ArticleListView.Factory articleListViewFactory() {
    ArticleListView.Factory mockArticleListViewFactory = spy(ArticleListView.Factory.class);
    LOG.debug("articleListViewFactory() * --> {}", mockArticleListViewFactory);
    return mockArticleListViewFactory;
  }

  @Bean
  public ArticleRevisionView.Factory articleRevisionViewFactory() {
    ArticleRevisionView.Factory mockArticleRevisionViewFactory = spy(ArticleRevisionView.Factory.class);
    LOG.debug("articleRevisionViewFactory() * --> {}", mockArticleRevisionViewFactory);
    return mockArticleRevisionViewFactory;
  }

  protected ArticleListIdentity createStubArticleListIdentity() {
    return createStubArticleListIdentity(ARTICLE_LIST_ID_TYPE, ARTICLE_LIST_ID_JOURNAL, ARTICLE_LIST_ID_KEY);
  }

  protected ArticleListIdentity createStubArticleListIdentity(String type, String journal, String key) {
    return new ArticleListIdentity(type, journal, key);
  }

  protected ArticleListView createStubArticleList() {
    return createStubArticleList(null, null, null);
  }

  protected ArticleListView createStubArticleList(String displayName, ArticleListIdentity identity, Article article) {
    if (identity == null) {
      identity = createStubArticleListIdentity();
    }
    if (displayName == null) {
      displayName = ARTICLE_LIST_TITLE;
    }
    Set<ArticleIdentifier> articleIds = new HashSet<>();
    Set<String> articleKeys = new HashSet<String>();

    when(mockHibernateTemplate.execute(any())).thenReturn(0L);
    when(mockHibernateTemplate.findByCriteria(any())).thenReturn(
        Collections.singletonList(new Journal(identity.getJournalKey())));

    if (article != null) {
      articleIds.add(ArticleIdentifier.create(article.getDoi()));
      articleKeys.add(article.getDoi());

      when(mockHibernateTemplate.findByNamedParam("from Article where doi in :articleKeys",
          "articleKeys", articleKeys)).thenReturn(
          new ArrayList(Collections.singletonList(article))
      );
    }

    ArticleListView articleListView = mockArticleListCrudService.create(identity, displayName, articleIds);

    ArticleList articleList = articleListView.getArticleList();
    articleList.setArticles(new ArrayList<Article>(articleList.getArticles()));

    return articleListView;
  }

  @Test
  public void testCreate_Succeed() throws Exception {
    ArticleListIdentity identity = createStubArticleListIdentity();
    String displayName = ARTICLE_LIST_TITLE;
    Set<ArticleIdentifier> articleIds = new HashSet<>();

    when(mockHibernateTemplate.execute(any())).thenReturn(0L);
    when(mockHibernateTemplate.findByCriteria(any())).thenReturn(
        Collections.singletonList(new Journal(identity.getJournalKey())));

    ArticleListView articleListView = mockArticleListCrudService.create(identity, displayName, articleIds);
    assertThat(articleListView.getIdentity()).isEqualTo(identity);
    assertThat(articleListView.getArticleList().getDisplayName()).isEqualTo(displayName);
    assertThat(articleListView.getArticleList().getArticles().size()).isEqualTo(articleIds.size());
  }

  @Test
  public void testCreate_FailForInvalidJournal() throws Exception {
    ArticleListIdentity identity = createStubArticleListIdentity(ARTICLE_LIST_ID_TYPE, "invalid-journal", ARTICLE_LIST_ID_KEY);
    String displayName = ARTICLE_LIST_TITLE;
    Set<ArticleIdentifier> articleIds = new HashSet<>();

    when(mockHibernateTemplate.execute(any())).thenReturn(0L).thenReturn(null);

    try {
      mockArticleListCrudService.create(identity, displayName, articleIds);
      fail("Expecting exception, but nothing was thrown.");
    } catch (Exception exception) {
      assertThat(exception).isInstanceOf(RestClientException.class);
      assertThat(exception).hasMessageThat().isEqualTo("Journal not found: " + identity.getJournalKey());
    }
  }

  @Test
  public void testCreate_FailForExistingArticleList() throws Exception {
    ArticleListIdentity identity = createStubArticleListIdentity();
    String displayName = ARTICLE_LIST_TITLE;
    Set<ArticleIdentifier> articleIds = new HashSet<>();

    when(mockHibernateTemplate.execute(any())).thenReturn(1L);

    try {
      mockArticleListCrudService.create(identity, displayName, articleIds);
      fail("Expecting exception, but nothing was thrown.");
    } catch (Exception exception) {
      assertThat(exception).isInstanceOf(RestClientException.class);
      assertThat(exception).hasMessageThat().isEqualTo("List already exists: " + identity);
    }
  }

  @Test
  public void testUpdate_SucceedForDisplayName() {
    ArticleListIdentity identity = createStubArticleListIdentity();
    ArticleListView articleListView = createStubArticleList();

    when(mockHibernateTemplate.execute(any())).thenReturn(
        new ArrayList(Collections.singletonList(new Object[] {
            "valid-journal", articleListView.getArticleList()
        })));

    String newDisplayName = "article list 2";
    ArticleListView newArticleListView = mockArticleListCrudService.update(identity, Optional.ofNullable(newDisplayName), Optional.empty());
    assertThat(newArticleListView.getIdentity()).isEqualTo(identity);
    assertThat(newArticleListView.getArticleList().getDisplayName()).isEqualTo(newDisplayName);
    assertThat(newArticleListView.getArticleList().getArticles()).isEmpty();

  }

  @Test
  public void testUpdate_SucceedForAddArticlesList() {
    ArticleListIdentity identity = createStubArticleListIdentity();
    ArticleListView articleListView = createStubArticleList();

    Article article = createStubArticle();
    Set<String> articleKeys = new HashSet<String>();
    articleKeys.add(article.getDoi());

    when(mockHibernateTemplate.execute(any())).thenReturn(
        new ArrayList(Collections.singletonList(new Object[] {
            "valid-journal", articleListView.getArticleList()
        })));

    when(mockHibernateTemplate.findByNamedParam("from Article where doi in :articleKeys",
        "articleKeys", articleKeys)).thenReturn(
        new ArrayList(Collections.singletonList(article))
    );

    Set<ArticleIdentifier> newArticleIds = new HashSet<>();
    newArticleIds.add(ArticleIdentifier.create(article.getDoi()));

    ArticleListView newArticleListView = mockArticleListCrudService.update(identity, Optional.empty(), Optional.ofNullable(newArticleIds));
    assertThat(newArticleListView.getIdentity()).isEqualTo(identity);
    assertThat(newArticleListView.getArticleList().getArticles().size()).isEqualTo(newArticleIds.size());
  }

  @Test
  public void testUpdate_SucceedForRemoveArticlesList() {
    ArticleListIdentity identity = createStubArticleListIdentity();
    ArticleListView articleListView = createStubArticleList(null, null, createStubArticle());

    when(mockHibernateTemplate.execute(any())).thenReturn(
        new ArrayList(Collections.singletonList(new Object[] {
            "valid-journal", articleListView.getArticleList()
        })));


    Set<String> articleKeys = new HashSet<String>();
    articleKeys.add(createStubArticleDoi().toString());

    when(mockHibernateTemplate.findByNamedParam("from Article where doi in :articleKeys",
        "articleKeys", articleKeys)).thenReturn(
        new ArrayList()
    );

    Set<ArticleIdentifier> newArticleIds = new HashSet<>();

    ArticleListView newArticleListView = mockArticleListCrudService.update(identity, Optional.empty(), Optional.ofNullable(newArticleIds));
    assertThat(newArticleListView.getIdentity()).isEqualTo(identity);
    assertThat(newArticleListView.getArticleList().getArticles().size()).isEqualTo(newArticleIds.size());
  }

  @Test
  public void testRead_FailForNonExistent() throws Exception {
    ArticleListIdentity identity = createStubArticleListIdentity("invalid-type", "invalid-journal", "invalid-key");
    try {
      mockArticleListCrudService.read(identity);
      fail("Expecting exception, but nothing was thrown.");
    } catch (Exception exception) {
      assertThat(exception).isInstanceOf(RestClientException.class);
      assertThat(exception).hasMessageThat().isEqualTo("List does not exist: " + identity);
    }
  }

  @Test
  public void testRead_Succeed() throws Exception {
    ArticleListIdentity identity = createStubArticleListIdentity();
    ArticleListView articleListView = createStubArticleList();
    ArticleList articleList = articleListView.getArticleList();

    when(mockHibernateTemplate.execute(any())).thenReturn(
        new ArrayList(Collections.singletonList(new Object[] {
            "valid-journal", articleList
        })));

    ServiceResponse<ArticleListView> response = mockArticleListCrudService.read(identity);

    ArticleListView newArticleListView = response.getBody();
    assertThat(newArticleListView.getIdentity()).isEqualTo(identity);
    assertThat(newArticleListView.getArticleList()).isEqualTo(articleList);

//    // commented out code is needed to test without modifying ServiceResponse.java
//    // and also needs SampleSerializer and localGson later.
//    ResponseEntity<?> entity = response.asJsonResponse(localGson());
//    String body = new String((byte[]) entity.getBody(), Charset.forName("UTF8"));
//    logger.debug(body);
  }

//  public static class SampleSerializer implements JsonSerializer<ArticleRevisionView.Factory>
//  {
//    public JsonElement serialize(ArticleRevisionView.Factory src, Type typeOfSrc, JsonSerializationContext context)
//    {
//      return null;
//    }
//  }
//
//  private Gson localGson() {
//    final GsonBuilder builder = JsonAdapterUtil.makeGsonBuilder().setPrettyPrinting();
//    Java8TimeGsonAdapters.register(builder);
//    builder.registerTypeAdapter(ArticleRevisionView.Factory.class, new SampleSerializer());
//    return builder.create();
//  }

  @Test
  public void testReadAll_SucceedForEmpty() {
    when(mockHibernateTemplate.execute(any())).thenReturn(
        new ArrayList()
    );

    ServiceResponse<Collection<ArticleListView>> response = mockArticleListCrudService.readAll();
    assertThat(response.getBody()).isEmpty();
  }


  @Test
  public void testReadAll_Succeed() {
    ArticleListIdentity identity1 = createStubArticleListIdentity();
    ArticleListView articleListView1 = createStubArticleList();
    ArticleList articleList1 = articleListView1.getArticleList();

    ArticleListIdentity identity2 = createStubArticleListIdentity("invalid-type", ARTICLE_LIST_ID_JOURNAL, ARTICLE_LIST_ID_KEY);
    ArticleListView articleListView2 = createStubArticleList("article list 2", identity2, null);
    ArticleList articleList2 = articleListView2.getArticleList();

    List<Object[]> list = new ArrayList<Object[]>();
    list.add(new Object[] {ARTICLE_LIST_ID_JOURNAL, articleList1});
    list.add(new Object[] {ARTICLE_LIST_ID_JOURNAL, articleList2});
    when(mockHibernateTemplate.execute(any())).thenReturn(list);

    ServiceResponse<Collection<ArticleListView>> response = mockArticleListCrudService.readAll();
    Collection<ArticleListView> body = response.getBody();
    assertThat(body.size()).isEqualTo(2);
    ArticleListView[] bodyArray = body.toArray(new ArticleListView[2]);
    assertThat(bodyArray[0].getIdentity()).isEqualTo(identity1);
    assertThat(bodyArray[0].getArticleList()).isEqualTo(articleList1);
    assertThat(bodyArray[1].getIdentity()).isEqualTo(identity2);
    assertThat(bodyArray[1].getArticleList()).isEqualTo(articleList2);

    list = new ArrayList<Object[]>();
    list.add(new Object[] {ARTICLE_LIST_ID_JOURNAL, articleList1});
    when(mockHibernateTemplate.execute(any())).thenReturn(list);

    response = mockArticleListCrudService.readAll(ARTICLE_LIST_ID_TYPE, Optional.empty());
    body = response.getBody();
    assertThat(body.size()).isEqualTo(1);
    bodyArray = body.toArray(new ArticleListView[1]);
    assertThat(bodyArray[0].getIdentity()).isEqualTo(identity1);
    assertThat(bodyArray[0].getArticleList()).isEqualTo(articleList1);
  }


  @Test
  public void testReadContainingLists_SucceedForEmpty() {
    when(mockHibernateTemplate.execute(any())).thenReturn(
        new ArrayList()
    );

    ArticleIdentifier articleId = ArticleIdentifier.create(createStubArticleDoi());
    ServiceResponse<Collection<ArticleListView>> response = mockArticleListCrudService.readContainingLists(articleId);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  public void testReadContainingLists_Succeed() {
    Article articleA = createStubArticle();
    Article articleB = createStubArticle(1L, "second-doi");

    ArticleListIdentity identity1 = createStubArticleListIdentity();
    ArticleListView articleListView1 = createStubArticleList(null, identity1, articleA);
    ArticleList articleList1 = articleListView1.getArticleList();

    ArticleListIdentity identity2 = createStubArticleListIdentity("second-type", ARTICLE_LIST_ID_JOURNAL, ARTICLE_LIST_ID_KEY);
    ArticleListView articleListView2 = createStubArticleList("article list 2", identity2, articleA);
    ArticleList articleList2 = articleListView2.getArticleList();

    ArticleListIdentity identity3 = createStubArticleListIdentity("third-type", ARTICLE_LIST_ID_JOURNAL, ARTICLE_LIST_ID_KEY);
    ArticleListView articleListView3 = createStubArticleList("article list 3", identity3, articleB);
    ArticleList articleList3 = articleListView3.getArticleList();

    List<Object[]> list1 = new ArrayList();
    list1.add(new Object[]{ ARTICLE_LIST_ID_JOURNAL, articleList1});
    list1.add(new Object[]{ ARTICLE_LIST_ID_JOURNAL, articleList2});

    ArticleIdentifier articleIdA = ArticleIdentifier.create(articleA.getDoi());

    when(mockHibernateTemplate.execute(any())).thenReturn(list1);

    ServiceResponse<Collection<ArticleListView>> response = mockArticleListCrudService.readContainingLists(articleIdA);
    Collection<ArticleListView> listA = response.getBody();
    assertThat(listA.size()).isEqualTo(list1.size());
    assertThat(listA.contains(articleListView1));
    assertThat(listA.contains(articleListView2));

    List<Object[]> list2 = new ArrayList();
    list2.add(new Object[]{ ARTICLE_LIST_ID_JOURNAL, articleList3});

    ArticleIdentifier articleIdB = ArticleIdentifier.create(articleB.getDoi());

    when(mockHibernateTemplate.execute(any())).thenReturn(list2);

    response = mockArticleListCrudService.readContainingLists(articleIdB);
    Collection<ArticleListView> listB = response.getBody();
    assertThat(listB.size()).isEqualTo(list2.size());
    assertThat(listB.contains(articleListView3));
  }
}

