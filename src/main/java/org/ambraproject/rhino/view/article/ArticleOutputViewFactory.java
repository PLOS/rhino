package org.ambraproject.rhino.view.article;

import com.google.common.collect.ImmutableList;
import org.ambraproject.models.Article;
import org.ambraproject.models.Pingback;
import org.ambraproject.models.Syndication;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleType;
import org.ambraproject.rhino.service.ArticleTypeService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.PingbackReadService;
import org.ambraproject.service.article.NoSuchArticleIdException;
import org.ambraproject.service.syndication.SyndicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;

public class ArticleOutputViewFactory {

  private static final Logger log = LoggerFactory.getLogger(ArticleOutputViewFactory.class);

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private SyndicationService syndicationService;
  @Autowired
  private PingbackReadService pingbackReadService;
  @Autowired
  private ArticleTypeService articleTypeService;
  @Autowired
  private IssueCrudService issueCrudService;
  @Autowired
  private AnnotationCrudService annotationCrudService;

  /**
   * Creates a new view of the given article and associated data.
   *
   * @param article          primary entity
   * @param excludeCitations if true, don't serialize citation information
   * @return view of the article and associated data
   */
  public ArticleOutputView create(Article article, boolean excludeCitations) {
    final ArticleIdentity articleIdentity = ArticleIdentity.create(article);

    Collection<RelatedArticleView> relatedArticles = articleCrudService.getRelatedArticles(article);
    Collection<Syndication> syndications;
    try {
      syndications = syndicationService.getSyndications(article.getDoi());
    } catch (NoSuchArticleIdException e) {
      throw new RuntimeException(e); // should be impossible
    }
    if (syndications == null) {
      log.warn("SyndicationService.getSyndications returned null; assuming no syndications");
      syndications = ImmutableList.of();
    }

    String nlmArticleType = articleTypeService.getNlmArticleType(article);
    ArticleType articleType = articleTypeService.getArticleType(article);

    List<Pingback> pingbacks = pingbackReadService.loadPingbacks(article);

    List<ArticleIssue> articleIssues = issueCrudService.getArticleIssues(articleIdentity);

    long commentCount = annotationCrudService.getCommentCount(article);

    return new ArticleOutputView.AugmentedView(
        article,
        nlmArticleType,
        articleType,
        relatedArticles,
        articleIssues,
        syndications,
        pingbacks,
        commentCount,
        excludeCitations);
  }

}
