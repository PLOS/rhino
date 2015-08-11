package org.ambraproject.rhino.rest.controller;

import com.google.common.collect.Sets;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleListCrudService;
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
public class ArticleListCrudController extends RestController {

  @Autowired
  private ArticleListCrudService articleListCrudService;

  private static Set<ArticleIdentity> asArticleIdentities(String[] articleDois) {
    Set<ArticleIdentity> articleIdentities = Sets.newLinkedHashSetWithExpectedSize(articleDois.length);
    for (String articleDoi : articleDois) {
      articleIdentities.add(ArticleIdentity.create(articleDoi));
    }
    return articleIdentities;
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists", method = RequestMethod.POST)
  public ResponseEntity<?> create(@RequestParam("journal") String journalKey,
                                  @RequestParam("listCode") String listCode,
                                  @RequestParam("title") String title,
                                  @RequestParam("articles") String[] articleDois)
      throws IOException {
    ArticleListIdentity identity = new ArticleListIdentity(journalKey, listCode);
    articleListCrudService.create(identity, title, asArticleIdentities(articleDois));
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
  @RequestMapping(value = "/lists/{journal}/{listCode}", method = RequestMethod.PATCH)
  public ResponseEntity<?> update(@PathVariable("journal") String journalKey,
                                  @PathVariable("listCode") String listCode,
                                  @RequestParam(value = "title", required = false) String title,
                                  @RequestParam(value = "articles", required = false) String[] articleDois)
      throws IOException {
    Set<ArticleIdentity> articleIds = (articleDois == null || articleDois.length == 0) ? null
        : asArticleIdentities(articleDois);
    ArticleListIdentity identity = new ArticleListIdentity(journalKey, listCode);
    articleListCrudService.update(identity, title, articleIds);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{journal}/{listCode}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("journal") String journalKey,
                   @PathVariable("listCode") String listCode)
      throws IOException {
    ArticleListIdentity identity = new ArticleListIdentity(journalKey, listCode);
    articleListCrudService.read(identity).respond(request, response, entityGson);
  }

}
