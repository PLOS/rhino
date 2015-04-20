package org.ambraproject.rhino.rest.controller;

import com.google.common.io.ByteStreams;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.ArticleSpaceController;
import org.ambraproject.rhino.service.ArticleRevisionService;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Controller
public class ArticleRevisionController extends ArticleSpaceController {

  @Autowired
  private ArticleRevisionService articleRevisionService;

  @RequestMapping(value = "articles/revisions", method = RequestMethod.POST)
  public ResponseEntity<Object> createRevision(@RequestParam(value = ID_PARAM, required = true) String id,
                                               @RequestParam(value = VERSION_PARAM, required = false) Integer versionNumber,
                                               @RequestParam(value = REVISION_PARAM, required = true) Integer revisionNumber)
      throws IOException {
    ArticleIdentity articleIdentity = parse(id, versionNumber, null);
    articleRevisionService.createRevision(articleIdentity, revisionNumber);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }


  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/revisions", method = RequestMethod.GET, params = {ID_PARAM})
  public void list(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = ID_PARAM) String id)
      throws IOException {
    articleRevisionService.listRevisions(ArticleIdentity.create(id)).respond(request, response, entityGson);
  }


  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/revisions", method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteRevision(@RequestParam(value = ID_PARAM, required = true) String id,
                                          @RequestParam(value = REVISION_PARAM, required = true) Integer revisionNumber)
      throws IOException {
    ArticleIdentity articleIdentity = ArticleIdentity.create(id);
    boolean result = articleRevisionService.deleteRevision(articleIdentity, revisionNumber);
    if (!result) {
      throw new RestClientException("Revision doesn't exist", HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/associated", method = RequestMethod.GET)
  public ResponseEntity<String> findAssociatedArticle(@RequestParam("doi") String doi) {
    String parentArticleDoi = articleRevisionService.getParentDoi(doi);
    return (parentArticleDoi == null) ? new ResponseEntity<String>(HttpStatus.NOT_FOUND)
        : new ResponseEntity<>(entityGson.toJson(parentArticleDoi, String.class), HttpStatus.OK);
  }

  // TODO: Unify API with ArticleAsset
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/versionedAsset", method = RequestMethod.GET)
  public void readFileRevision(HttpServletRequest request, HttpServletResponse response,
                               @RequestParam(ID_PARAM) String assetDoi,
                               @RequestParam(REVISION_PARAM) int revisionNumber,
                               @RequestParam("repr") String repr)
      throws IOException {
    AssetIdentity assetIdentity = AssetIdentity.create(assetDoi);
    RepoObjectMetadata objectVersion = articleRevisionService.getObjectVersion(assetIdentity, repr, revisionNumber);
    streamRepoObject(response, objectVersion);
  }

}
