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
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ArticleListCrudController extends RestController {

  @Autowired
  private ArticleListCrudService articleListCrudService;

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists", method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request,
                                  @RequestParam(value = "listType", required = false) String listType,
                                  @RequestParam(value = "journal", required = true) String journalKey,
                                  @RequestParam(value = "listCode", required = true) String listCode)
      throws IOException {
    ListInputView inputView = readJsonFromRequest(request, ListInputView.class);
    Optional<String> title = inputView.getTitle();
    if (!title.isPresent()) {
      throw new RestClientException("title required", HttpStatus.BAD_REQUEST);
    }
    Optional<ImmutableSet<ArticleIdentity>> articleDois = inputView.getArticleIds();
    if (!articleDois.isPresent()) {
      throw new RestClientException("articleDois required", HttpStatus.BAD_REQUEST);
    }

    ArticleListIdentity identity = new ArticleListIdentity(Optional.fromNullable(listType), journalKey, listCode);
    articleListCrudService.create(identity, title.get(), articleDois.get());
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{journal}/{listCode}", method = RequestMethod.PATCH)
  public ResponseEntity<?> update(HttpServletRequest request,
                                  @PathVariable("journal") String journalKey,
                                  @PathVariable("listCode") String listCode)
      throws IOException {
    return update(request, null, journalKey, listCode);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{listType}/{journal}/{listCode}", method = RequestMethod.PATCH)
  public ResponseEntity<?> update(HttpServletRequest request,
                                  @PathVariable("listType") String listType,
                                  @PathVariable("journal") String journalKey,
                                  @PathVariable("listCode") String listCode)
      throws IOException {
    ListInputView inputView = readJsonFromRequest(request, ListInputView.class);
    Optional<String> listTypeObj = Optional.fromNullable(listType);
    ArticleListIdentity identity = new ArticleListIdentity(listTypeObj, journalKey, listCode);
    articleListCrudService.update(identity, inputView.getTitle(), inputView.getArticleIds());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{journal}/{listCode}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("journal") String journalKey,
                   @PathVariable("listCode") String listCode)
      throws IOException {
    read(request, response, null, journalKey, listCode);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/lists/{listType}/{journal}/{listCode}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("listType") String listType,
                   @PathVariable("journal") String journalKey,
                   @PathVariable("listCode") String listCode)
      throws IOException {
    ArticleListIdentity identity = new ArticleListIdentity(Optional.fromNullable(listType), journalKey, listCode);
    articleListCrudService.read(identity).respond(request, response, entityGson);
  }

}
