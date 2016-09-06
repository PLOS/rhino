package org.ambraproject.rhino.view.journal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.versioned.ArticleRevisionView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class IssueOutputView implements JsonOutputView {

  public static class Factory {
    @Autowired
    private ArticleCrudService articleCrudService;
    @Autowired
    private IssueCrudService issueCrudService;
    @Autowired
    private ArticleRevisionView.Factory articleRevisionViewFactory;

    private List<ArticleRevisionView> getIssueArticles(Issue issue) {
      List<ArticleTable> articles = issue.getArticles();
      if (articles == null) return ImmutableList.of();
      return articles.stream()
          .map(articleRevisionViewFactory::getLatestRevisionView)
          .collect(Collectors.toList());
    }

    public IssueOutputView getView(Issue issue) {
      return new IssueOutputView(this, issue, issueCrudService.getParentVolumeView(issue), getIssueArticles(issue));
    }

    public IssueOutputView getView(Issue issue, Volume parentVolume) {
      return new IssueOutputView(this, issue, new VolumeNonAssocView(parentVolume), getIssueArticles(issue));
    }
  }

  private final Issue issue;
  private final VolumeNonAssocView parentVolumeView;
  private final IssueOutputView.Factory factory;
  private final ImmutableList<ArticleRevisionView> articles;

  private IssueOutputView(Factory factory, Issue issue, VolumeNonAssocView parentVolumeView, List<ArticleRevisionView> articles) {
    this.issue = Objects.requireNonNull(issue);
    this.parentVolumeView = Objects.requireNonNull(parentVolumeView);
    this.factory = Objects.requireNonNull(factory);
    this.articles = ImmutableList.copyOf(articles);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(issue).getAsJsonObject();
    serialized.add("articles", context.serialize(articles));
    serialized.add("parentVolume", context.serialize(parentVolumeView));

    JsonElement imageArticle = serialized.get("imageArticle");
    if (imageArticle != null) {
      imageArticle.getAsJsonObject().addProperty("figureImageDoi", getIssueImageFigureDoi());
    }

    return serialized;
  }

  private static final ImmutableSet<String> FIGURE_IMAGE_TYPES = ImmutableSet.of("figure", "table");

  private String getIssueImageFigureDoi() {
    ArticleTable imageArticle = issue.getImageArticle();
    ArticleRevision latestArticleRevision = factory.articleCrudService.getLatestRevision(imageArticle).orElseThrow(
        () -> new RuntimeException("Image article has no published revisions. " + imageArticle.getDoi()));
    ArticleIngestion ingestion = latestArticleRevision.getIngestion();
    Collection<ArticleItem> allArticleItems = factory.articleCrudService.getAllArticleItems(ingestion);
    List<ArticleItem> figureImageItems = allArticleItems.stream()
        .filter(item -> FIGURE_IMAGE_TYPES.contains(item.getItemType())).collect(Collectors.toList());
    if (figureImageItems.size() != 1) {
      throw new RuntimeException("Image article does not contain exactly one image file. " + imageArticle.getDoi());
    }
    return figureImageItems.get(0).getDoi();
  }

}