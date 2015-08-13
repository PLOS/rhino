package org.ambraproject.rhino.rest.controller;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.CollectionCrudService;
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
import java.util.List;
import java.util.Set;

@Controller
public class CollectionCrudController extends RestController {

  @Autowired
  private CollectionCrudService collectionCrudService;

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/collections", method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request,
                                  @RequestParam("journal") String journalKey,
                                  @RequestParam("slug") String slug)
      throws IOException {
    CollectionInputView inputView = readJsonFromRequest(request, CollectionInputView.class);
    Optional<String> title = inputView.getTitle();
    if (!title.isPresent()){
      throw new RestClientException("title required",HttpStatus.BAD_REQUEST);
    }
    Optional<ImmutableSet<ArticleIdentity>> articleDois = inputView.getArticleIds();
    if (!articleDois.isPresent()){
      throw new RestClientException("articleDois required",HttpStatus.BAD_REQUEST);
    }

    collectionCrudService.create(journalKey, slug, title.get(), articleDois.get());
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
  @RequestMapping(value = "/collections/{journal}/{slug}", method = RequestMethod.PATCH)
  public ResponseEntity<?> update(HttpServletRequest request,
                                  @PathVariable("journal") String journalKey,
                                  @PathVariable("slug") String slug                                  )
      throws IOException {
    CollectionInputView inputView = readJsonFromRequest(request, CollectionInputView.class);
    collectionCrudService.update(journalKey, slug, inputView.getTitle(), inputView.getArticleIds());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/collections/{journal}/{slug}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("journal") String journalKey,
                   @PathVariable("slug") String slug)
      throws IOException {
    collectionCrudService.read(journalKey, slug).respond(request, response, entityGson);
  }

}
