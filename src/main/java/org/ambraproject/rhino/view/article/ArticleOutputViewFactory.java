package org.ambraproject.rhino.view.article;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Pingback;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleType;
import org.ambraproject.rhino.service.ArticleTypeService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.PingbackReadService;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
//todo: remove this class
public class ArticleOutputViewFactory {

  private static final Logger log = LoggerFactory.getLogger(ArticleOutputViewFactory.class);

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private SyndicationCrudService syndicationService;
  @Autowired
  private PingbackReadService pingbackReadService;
  @Autowired
  private ArticleTypeService articleTypeService;
  @Autowired
  private IssueCrudService issueCrudService;

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
    //Collection<Syndication> syndications = syndicationService.getSyndications(null); //todo: get articleIdentifier
    Collection<Syndication> syndications = null;
    if (syndications == null) {
      log.warn("SyndicationCrudService.getSyndications returned null; assuming no syndications");
      syndications = ImmutableList.of();
    }

    String nlmArticleType = articleTypeService.getNlmArticleType(article);
    ArticleType articleType = articleTypeService.getArticleType(article);

    List<Pingback> pingbacks = pingbackReadService.loadPingbacks(article);


    return new ArticleOutputView.AugmentedView(
        article,
        nlmArticleType,
        articleType,
        relatedArticles,
        null,
        syndications,
        pingbacks,
        excludeCitations);
  }

}
