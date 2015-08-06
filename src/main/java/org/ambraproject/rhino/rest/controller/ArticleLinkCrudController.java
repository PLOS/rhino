package org.ambraproject.rhino.rest.controller;

import com.google.common.collect.Sets;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleLinkIdentity;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleLinkCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

@Controller
public class ArticleLinkCrudController extends RestController {

  @Autowired
  private ArticleLinkCrudService articleLinkCrudService;

  private static Set<ArticleIdentity> asArticleIdentities(String[] articleDois) {
    Set<ArticleIdentity> articleIdentities = Sets.newLinkedHashSetWithExpectedSize(articleDois.length);
    for (String articleDoi : articleDois) {
      articleIdentities.add(ArticleIdentity.create(articleDoi));
    }
    return articleIdentities;
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/links", method = RequestMethod.POST)
  public ResponseEntity<?> create(@RequestParam("type") String linkType,
                                  @RequestParam("journal") String journalKey,
                                  @RequestParam("target") String target,
                                  @RequestParam("title") String title,
                                  @RequestParam("articles") String[] articleDois)
      throws IOException {
    ArticleLinkIdentity identity = new ArticleLinkIdentity(linkType, journalKey, target);
    articleLinkCrudService.create(identity, title, asArticleIdentities(articleDois));
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  /*
   * API design note: This, for now, is inconsistent with our other PATCH method (on article state). This one uses
   * request parameters to be consistent with its POST. The other one uses a JSON request body to be consistent with its
   * GET.
   *
   * TODO: Make PATCH requests consistent once we decide how we want to do it
   */
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/links/{linkType}/{journal}/{target}", method = RequestMethod.PATCH)
  public ResponseEntity<?> update(@PathVariable("linkType") String linkType,
                                  @PathVariable("journal") String journalKey,
                                  @PathVariable("target") String target,
                                  @RequestParam(value = "title", required = false) String title,
                                  @RequestParam(value = "articles", required = false) String[] articleDois)
      throws IOException {
    Set<ArticleIdentity> articleIds = (articleDois == null || articleDois.length == 0) ? null
        : asArticleIdentities(articleDois);
    ArticleLinkIdentity identity = new ArticleLinkIdentity(linkType, journalKey, target);
    articleLinkCrudService.update(identity, title, articleIds);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/links/{linkType}/{journal}/{target}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("linkType") String linkType,
                   @PathVariable("journal") String journalKey,
                   @PathVariable("target") String target)
      throws IOException {
    ArticleLinkIdentity identity = new ArticleLinkIdentity(linkType, journalKey, target);
    articleLinkCrudService.read(identity).respond(request, response, entityGson);
  }

}
