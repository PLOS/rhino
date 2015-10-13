package org.ambraproject.rhino.rest.controller;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.view.article.ListInputView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ArticleListCrudController extends RestController {

  @Autowired
  private ArticleListCrudService articleListCrudService;

  private static RestClientException complainAboutRequiredListIdentity(Exception cause) {
    return new RestClientException("type, journal, and key are required", HttpStatus.BAD_REQUEST, cause);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists", method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request) throws IOException {
    final ListInputView inputView;
    try {
      inputView = readJsonFromRequest(request, ListInputView.class);
    } catch (ListInputView.PartialIdentityException e) {
      throw complainAboutRequiredListIdentity(e);
    }

    Optional<ArticleListIdentity> identity = inputView.getIdentity();
    if (!identity.isPresent()) {
      throw complainAboutRequiredListIdentity(null);
    }
    Optional<String> title = inputView.getTitle();
    if (!title.isPresent()) {
      throw new RestClientException("title required", HttpStatus.BAD_REQUEST);
    }
    Optional<ImmutableSet<ArticleIdentity>> articleDois = inputView.getArticleIds();
    if (!articleDois.isPresent()) {
      throw new RestClientException("articleDois required", HttpStatus.BAD_REQUEST);
    }

    articleListCrudService.create(identity.get(), title.get(), articleDois.get());
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  private static RestClientException complainAboutListIdentityOnPatch(Exception cause) {
    return new RestClientException("type, journal, and key cannot be changed with PATCH", HttpStatus.BAD_REQUEST, cause);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{type}/{journal}/{key}", method = RequestMethod.PATCH)
  public ResponseEntity<?> update(HttpServletRequest request,
                                  @PathVariable("type") String type,
                                  @PathVariable("journal") String journalKey,
                                  @PathVariable("key") String key)
      throws IOException {
    final ListInputView inputView;
    try {
      inputView = readJsonFromRequest(request, ListInputView.class);
    } catch (ListInputView.PartialIdentityException e) {
      throw complainAboutListIdentityOnPatch(e);
    }
    if (inputView.getIdentity().isPresent()) {
      throw complainAboutListIdentityOnPatch(null);
    }

    ArticleListIdentity identity = new ArticleListIdentity(type, journalKey, key);
    articleListCrudService.update(identity, inputView.getTitle(), inputView.getArticleIds());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists", method = RequestMethod.GET)
  public void listAll(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    articleListCrudService.readAll(Optional.<String>absent(), Optional.<String>absent()).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{type}", method = RequestMethod.GET)
  public void listAll(HttpServletRequest request, HttpServletResponse response,
                      @PathVariable("type") String type)
      throws IOException {
    articleListCrudService.readAll(Optional.of(type), Optional.<String>absent()).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{type}/{journal}", method = RequestMethod.GET)
  public void listAll(HttpServletRequest request, HttpServletResponse response,
                      @PathVariable("type") String type,
                      @PathVariable("journal") String journalKey)
      throws IOException {
    articleListCrudService.readAll(Optional.of(type), Optional.of(journalKey)).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{type}/{journal}/{key}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("type") String type,
                   @PathVariable("journal") String journalKey,
                   @PathVariable("key") String key)
      throws IOException {
    ArticleListIdentity identity = new ArticleListIdentity(type, journalKey, key);
    articleListCrudService.read(identity).respond(request, response, entityGson);
  }

}
