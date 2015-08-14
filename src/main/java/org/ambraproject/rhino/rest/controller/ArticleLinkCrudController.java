package org.ambraproject.rhino.rest.controller;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleLinkIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleLinkCrudService;
import org.ambraproject.rhino.view.article.CollectionInputView;
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

@Controller
public class ArticleLinkCrudController extends RestController {

  @Autowired
  private ArticleLinkCrudService articleLinkCrudService;

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/links", method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request,
                                  @RequestParam("type") String linkType,
                                  @RequestParam("journal") String journalKey,
                                  @RequestParam("target") String target)
      throws IOException {
    CollectionInputView inputView = readJsonFromRequest(request, CollectionInputView.class);
    Optional<String> title = inputView.getTitle();
    if (!title.isPresent()) {
      throw new RestClientException("title required", HttpStatus.BAD_REQUEST);
    }
    Optional<ImmutableSet<ArticleIdentity>> articleDois = inputView.getArticleIds();
    if (!articleDois.isPresent()) {
      throw new RestClientException("articleDois required", HttpStatus.BAD_REQUEST);
    }

    ArticleLinkIdentity identity = new ArticleLinkIdentity(linkType, journalKey, target);
    articleLinkCrudService.create(identity, title.get(), articleDois.get());
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
  public ResponseEntity<?> update(HttpServletRequest request,
                                  @PathVariable("linkType") String linkType,
                                  @PathVariable("journal") String journalKey,
                                  @PathVariable("target") String target)
      throws IOException {
    CollectionInputView inputView = readJsonFromRequest(request, CollectionInputView.class);
    ArticleLinkIdentity identity = new ArticleLinkIdentity(linkType, journalKey, target);
    articleLinkCrudService.update(identity, inputView.getTitle(), inputView.getArticleIds());
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
