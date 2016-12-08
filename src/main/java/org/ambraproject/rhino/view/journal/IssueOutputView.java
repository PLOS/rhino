package org.ambraproject.rhino.view.journal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleRevisionView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class IssueOutputView implements JsonOutputView {

  public static class Factory {
    @Autowired
    private ArticleCrudService articleCrudService;
    @Autowired
    private IssueCrudService issueCrudService;
    @Autowired
    private ArticleRevisionView.Factory articleRevisionViewFactory;
    @Autowired
    private VolumeOutputView.Factory volumeOutputViewFactory;

    public List<ArticleRevisionView> getIssueArticlesView(Issue issue) {
      List<Article> articles = issue.getArticles();
      if (articles == null) return ImmutableList.of();
      return articles.stream()
          .map(articleRevisionViewFactory::getLatestRevisionView)
          .collect(Collectors.toList());
    }

    public IssueOutputView getView(Issue issue) {
      return getView(issue, issueCrudService.getParentVolume(issue));
    }

    public IssueOutputView getView(Issue issue, Volume parentVolume) {
      return new IssueOutputView(issue, parentVolume, this, volumeOutputViewFactory);
    }

    public Optional<IssueOutputView> getCurrentIssueViewFor(Journal journal) {
      return Optional.ofNullable(journal.getCurrentIssue()).map(this::getView);
    }
  }

  private final Issue issue;
  private final Volume parentVolume;
  private final IssueOutputView.Factory issueOutputViewFactory;
  private final VolumeOutputView.Factory volumeOutputViewFactory;

  private IssueOutputView(Issue issue, Volume parentVolume, Factory issueOutputViewFactory,
                          VolumeOutputView.Factory volumeOutputViewFactory) {
    this.issue = Objects.requireNonNull(issue);
    this.parentVolume = Objects.requireNonNull(parentVolume);
    this.issueOutputViewFactory = Objects.requireNonNull(issueOutputViewFactory);
    this.volumeOutputViewFactory = Objects.requireNonNull(volumeOutputViewFactory);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", issue.getDoi());
    serialized.addProperty("displayName", issue.getDisplayName());
    serialized.add("parentVolume", context.serialize(volumeOutputViewFactory.getView(parentVolume)));

    Article imageArticle = issue.getImageArticle();
    if (imageArticle != null) {
      JsonObject serializedImageArticle = new JsonObject();
      String figureImageDoi = getIssueImageFigureDoi(issueOutputViewFactory.articleCrudService, imageArticle);

      serializedImageArticle.addProperty("doi", imageArticle.getDoi());
      serializedImageArticle.addProperty("figureImageDoi", figureImageDoi);
      serialized.add("imageArticle", serializedImageArticle);
    }

    return serialized;
  }

  private static final ImmutableSet<String> FIGURE_IMAGE_TYPES = ImmutableSet.of("figure", "table");

  private static String getIssueImageFigureDoi(ArticleCrudService articleCrudService, Article imageArticle) {
    ArticleRevision latestArticleRevision = articleCrudService.getLatestRevision(imageArticle).orElseThrow(
        () -> new RuntimeException("Image article has no published revisions. " + imageArticle.getDoi()));
    ArticleIngestion ingestion = latestArticleRevision.getIngestion();
    Collection<ArticleItem> allArticleItems = articleCrudService.getAllArticleItems(ingestion);
    List<ArticleItem> figureImageItems = allArticleItems.stream()
        .filter(item -> FIGURE_IMAGE_TYPES.contains(item.getItemType())).collect(Collectors.toList());
    if (figureImageItems.size() != 1) {
      throw new RuntimeException("Image article does not contain exactly one image file. " + imageArticle.getDoi());
    }
    return figureImageItems.get(0).getDoi();
  }

}