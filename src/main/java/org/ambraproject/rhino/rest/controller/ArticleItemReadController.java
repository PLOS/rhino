package org.ambraproject.rhino.rest.controller;

import com.google.gson.Gson;
import org.ambraproject.rhino.identity.CommentIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.identity.IssueIdentifier;
import org.ambraproject.rhino.identity.VolumeIdentifier;
import org.ambraproject.rhino.model.Comment;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.ResolvedDoiView;
import org.ambraproject.rhino.view.article.ArticleOverview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Calendar;
import java.util.Optional;

@Controller
public class ArticleItemReadController extends RestController {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private VolumeCrudService volumeCrudService;
  @Autowired
  private IssueCrudService issueCrudService;
  @Autowired
  private CommentCrudService commentCrudService;
  @Autowired
  private Gson entityGson;

  @Transactional(readOnly = true)
  @RequestMapping(value = "/dois/{doi:.+}", method = RequestMethod.GET)
  public void resolve(HttpServletRequest request, HttpServletResponse response,
                      @PathVariable("doi") String escapedDoi)
      throws IOException {
    Doi doi = DoiEscaping.unescape(escapedDoi);
    asTransceiver(findDoiTarget(doi)).respond(request, response, entityGson);
  }

  private Transceiver asTransceiver(ResolvedDoiView view) {
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        return view;
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  private ResolvedDoiView findDoiTarget(Doi doi) throws IOException {
    // Look for the DOI in the corpus of articles
    Optional<ResolvedDoiView> itemOverview = articleCrudService.getItemOverview(doi);
    if (itemOverview.isPresent()) {
      return itemOverview.get();
    }

    // Not found as an Article or ArticleItem. Check other object types that use DOIs.
    Optional<Comment> comment = commentCrudService.getComment(CommentIdentifier.create(doi));
    if (comment.isPresent()) {
      ArticleOverview article = articleCrudService.buildOverview(comment.get().getArticle());
      return ResolvedDoiView.createForArticle(doi, ResolvedDoiView.DoiWorkType.COMMENT, article);
    }
    Optional<Issue> issue = issueCrudService.getIssue(IssueIdentifier.create(doi));
    if (issue.isPresent()) {
      Journal journal = issueCrudService.getJournalOf(issue.get());
      return ResolvedDoiView.create(doi, ResolvedDoiView.DoiWorkType.ISSUE, journal);
    }
    Optional<Volume> volume = volumeCrudService.getVolume(VolumeIdentifier.create(doi));
    if (volume.isPresent()) {
      Journal journal = volumeCrudService.getJournalOf(volume.get());
      return ResolvedDoiView.create(doi, ResolvedDoiView.DoiWorkType.VOLUME, journal);
    }
    throw new RestClientException("DOI not found: " + doi.getName(), HttpStatus.NOT_FOUND);
  }

}
