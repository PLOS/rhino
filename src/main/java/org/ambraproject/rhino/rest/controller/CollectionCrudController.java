package org.ambraproject.rhino.rest.controller;

import com.google.common.collect.Sets;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.CollectionCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Set;

@Controller
public class CollectionCrudController extends RestController {

  @Autowired
  private CollectionCrudService collectionCrudService;

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/collections", method = RequestMethod.POST)
  public ResponseEntity<?> create(@RequestParam("slug") String slug,
                                  @RequestParam("journal") String journalKey,
                                  @RequestParam("title") String title,
                                  @RequestParam("articles") String[] articleDois)
      throws IOException {
    Set<ArticleIdentity> articleIdentities = Sets.newLinkedHashSetWithExpectedSize(articleDois.length);
    for (String articleDoi : articleDois) {
      articleIdentities.add(ArticleIdentity.create(articleDoi));
    }

    collectionCrudService.create(slug, journalKey, title, articleIdentities);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

}
