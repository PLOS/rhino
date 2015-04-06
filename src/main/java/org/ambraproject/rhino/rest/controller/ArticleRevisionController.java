package org.ambraproject.rhino.rest.controller;

import com.google.common.io.ByteStreams;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleRevisionService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.model.RepoVersionNumber;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Controller
public class ArticleRevisionController extends RestController {

  @Autowired
  private ArticleRevisionService articleRevisionService;

  // These shouldn't stay here. I'm violating layering for the sake of prototype tinkering.
  @Autowired
  private HibernateTemplate hibernateTemplate;
  @Autowired
  private ContentRepoService versionedContentRepoService;

  @RequestMapping(value = "articles/revisions/create", method = RequestMethod.GET)
  public void createRevision(HttpServletRequest request, HttpServletResponse response,
                             @RequestParam(value = "id", required = true) String doi,
                             @RequestParam(value = "r", required = true) Integer revisionNumber)
      throws IOException {

    // Just a dumb placeholder. TODO: Remove
    // This violates the idempotence of GET, obviously.

    ArticleRevision r = new ArticleRevision();
    r.setDoi(doi);
    r.setRevisionNumber(revisionNumber);
    r.setCrepoUuid(UUID.randomUUID().toString());
    hibernateTemplate.persist(r);
  }


  private ArticleRevision getLatestRevision(final String doi) {
    return hibernateTemplate.execute(new HibernateCallback<ArticleRevision>() {
      @Override
      public ArticleRevision doInHibernate(Session session) {
        Query query = session.createQuery("from ArticleRevision where doi=? order by revisionNumber desc");
        query.setString(0, doi);
        query.setMaxResults(1);
        return (ArticleRevision) DataAccessUtils.singleResult(query.list());
      }
    });
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "articles/revisions", method = RequestMethod.GET)
  public void findRevision(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam(value = "id", required = true) String doi,
                           @RequestParam(value = "r", required = false) Integer revisionNumber)
      throws IOException {

    // TODO: Pull into service class
    // For prototype, just using this method as a development sandbox

    String uuid;
    if (revisionNumber == null) {
      uuid = getLatestRevision(doi).getCrepoUuid();
    } else {
      uuid = (String) DataAccessUtils.uniqueResult(
          hibernateTemplate.find("select crepoUuid from ArticleRevision where doi=? and revisionNumber=?",
              doi, revisionNumber));
    }
    if (uuid == null) {
      StringBuilder message = new StringBuilder().append("No revision exists with id=").append(doi);
      if (revisionNumber != null) {
        message.append(" and r=").append(revisionNumber);
      }
      throw new RestClientException(message.toString(), HttpStatus.NOT_FOUND);
    }

    try (PrintWriter writer = response.getWriter()) {
      writer.println(uuid);
    }
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/versions", method = RequestMethod.GET, params = {"doi"})
  public void list(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam("doi") String doi)
      throws IOException {
    articleRevisionService.listRevisions(ArticleIdentity.create(doi)).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/versions", method = RequestMethod.GET, params = {"doi", "r"})
  public void readRevision(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam("doi") String doi,
                           @RequestParam("r") int revisionNumber)
      throws IOException {
    String uuid = (String) DataAccessUtils.uniqueResult(hibernateTemplate.find(
        "select crepoUuid from ArticleRevision where doi=? and revisionNumber=?", doi, revisionNumber));
    if (uuid == null) throw new RestClientException("Not found", HttpStatus.NOT_FOUND);
    articleRevisionService.readVersion(ArticleIdentity.create(doi), UUID.fromString(uuid)).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/versions", method = RequestMethod.GET, params = {"doi", "v"})
  public void readVersion(HttpServletRequest request, HttpServletResponse response,
                          @RequestParam("doi") String doi,
                          @RequestParam("v") int versionNumber)
      throws IOException {
    String uuid = (String) DataAccessUtils.uniqueResult(hibernateTemplate.find(
        "select crepoUuid from ArticleRevision where doi=? and versionNumber=?", doi, versionNumber));
    if (uuid == null) throw new RestClientException("Not found", HttpStatus.NOT_FOUND);
    articleRevisionService.readVersion(ArticleIdentity.create(doi), UUID.fromString(uuid)).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/versions/ingest", method = RequestMethod.POST)
  public void ingest(HttpServletRequest request, HttpServletResponse response,
                     @RequestParam("archive") MultipartFile requestFile)
      throws IOException {
    try (InputStream requestStream = requestFile.getInputStream()) {
      articleRevisionService.ingest(requestStream);
    } catch (XmlContentException e) {
      throw new RestClientException("Invalid XML", HttpStatus.BAD_REQUEST, e);
    }
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/revisions", method = RequestMethod.POST)
  public ResponseEntity<?> createRevision(HttpServletRequest request, HttpServletResponse response,
                                          @RequestParam(value = "doi", required = true) String doi,
                                          @RequestParam(value = "v", required = true) int versionNumber,
                                          @RequestParam(value = "r", required = false) Integer revisionNumber)
      throws IOException {
    RepoCollectionMetadata collectionMetadata = versionedContentRepoService.getCollection(new RepoVersionNumber(doi, versionNumber));
    // TODO: Throw RestClientException if collection does not exist

    ArticleRevision revision;
    if (revisionNumber == null) {
      ArticleRevision latestRevision = getLatestRevision(doi); // TODO: Transaction safety
      int newRevisionNumber = (latestRevision == null) ? 1 : latestRevision.getRevisionNumber() + 1;

      revision = new ArticleRevision();
      revision.setDoi(doi);
      revision.setRevisionNumber(newRevisionNumber);
    } else {
      revision = (ArticleRevision) DataAccessUtils.uniqueResult(
          hibernateTemplate.find("from ArticleRevision where doi=? and revisionNumber=?",
              doi, revisionNumber));
      if (revision == null) {
        revision = new ArticleRevision();
        revision.setDoi(doi);
        revision.setRevisionNumber(revisionNumber);
      }
    }
    revision.setCrepoUuid(collectionMetadata.getVersion().getUuid().toString());
    hibernateTemplate.persist(revision);

    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/revisions", method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteRevision(HttpServletRequest request, HttpServletResponse response,
                                          @RequestParam(value = "doi", required = true) String doi,
                                          @RequestParam(value = "r", required = true) int revisionNumber)
      throws IOException {
    ArticleRevision revision = (ArticleRevision) DataAccessUtils.uniqueResult(
        hibernateTemplate.find("from ArticleRevision where doi=? and revisionNumber=?",
            doi, revisionNumber));
    if (revision == null) {
      throw new RestClientException("Revision doesn't exist", HttpStatus.NOT_FOUND);
    }
    hibernateTemplate.delete(revision);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/associated", method = RequestMethod.GET)
  public ResponseEntity<String> findAssociatedArticle(HttpServletRequest request, HttpServletResponse response,
                                                      @RequestParam("doi") String doi) {
    String parentArticleDoi = (String) DataAccessUtils.uniqueResult(hibernateTemplate.find(
        "select parentArticleDoi from ArticleAssociation where doi=?", doi));
    return (parentArticleDoi == null) ? new ResponseEntity<String>(HttpStatus.NOT_FOUND)
        : new ResponseEntity<>(entityGson.toJson(parentArticleDoi, String.class), HttpStatus.OK);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "articles/versionedAsset", method = RequestMethod.GET)
  public void readFileRevision(HttpServletRequest request, HttpServletResponse response,
                               @RequestParam("doi") String assetDoi,
                               @RequestParam("repr") String repr,
                               @RequestParam("r") int revisionNumber)
      throws IOException {
    String parentArticleDoi = (String) DataAccessUtils.uniqueResult(hibernateTemplate.find(
        "select parentArticleDoi from ArticleAssociation where doi=?", assetDoi));
    if (parentArticleDoi == null) throw new RestClientException("Unrecognized asset DOI", HttpStatus.NOT_FOUND);

    String parentArticleUuid = (String) DataAccessUtils.uniqueResult(hibernateTemplate.find(
        "select crepoUuid from ArticleRevision where doi=? and revisionNumber=?", parentArticleDoi, revisionNumber));
    if (parentArticleUuid == null) throw new RestClientException("Revision not found", HttpStatus.NOT_FOUND);

    RepoCollectionMetadata articleCollection = versionedContentRepoService.getCollection(RepoVersion.create(parentArticleDoi, parentArticleUuid));
    Map<String, ?> articleMetadata = (Map<String, ?>) articleCollection.getJsonUserMetadata().get();
    Collection<Map<String, ?>> assets = (Collection<Map<String, ?>>) articleMetadata.get("assets");
    AssetIdentity targetAssetId = AssetIdentity.create(assetDoi);
    RepoVersion assetVersion = findAssetVersion(repr, assets, targetAssetId);

    streamFile(request, response, assetVersion);
  }

  private static RepoVersion findAssetVersion(String repr, Collection<Map<String, ?>> assets, AssetIdentity targetAssetId) {
    for (Map<String, ?> asset : assets) {
      AssetIdentity assetId = AssetIdentity.create((String) asset.get("doi"));
      if (assetId.equals(targetAssetId)) {
        Map<String, ?> assetObjects = (Map<String, ?>) asset.get("objects");
        Map<String, ?> assetRepr = (Map<String, ?>) assetObjects.get(repr);
        if (assetRepr == null) throw new RestClientException("repr not found", HttpStatus.NOT_FOUND);
        return RepoVersion.create((String) assetRepr.get("key"), (String) assetRepr.get("uuid"));
      }
    }
    // We already match the asset DOI to this article in the ArticleAssociation table,
    // but this is still possible if the asset appears in other revisions of the article but not this one.
    throw new RestClientException("Asset not in revision", HttpStatus.NOT_FOUND);
  }


  private void streamFile(HttpServletRequest request, HttpServletResponse response, RepoVersion repoVersion) throws IOException {
    // TODO: Respect headers, reproxying, etc. This is just prototype code.
    // See org.ambraproject.rhino.rest.controller.AssetFileCrudController.read
    response.setStatus(HttpStatus.OK.value());
    try (InputStream fileStream = versionedContentRepoService.getRepoObject(repoVersion);
         OutputStream responseStream = response.getOutputStream()) {
      ByteStreams.copy(fileStream, responseStream);
    }
  }

}
