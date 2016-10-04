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
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleRevisionView;
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
      List<Article> articles = issue.getArticles();
      if (articles == null) return ImmutableList.of();
      return articles.stream()
          .map(articleRevisionViewFactory::getLatestRevisionView)
          .collect(Collectors.toList());
    }

    public IssueOutputView getView(Issue issue) {
      return new DeepView(issue, issueCrudService.getParentVolume(issue), getIssueArticles(issue), this);
    }
  }

  public static IssueOutputView getShallowView(Issue issue) {
    return new IssueOutputView(issue);
  }


  protected final Issue issue;

  private IssueOutputView(Issue issue) {
    this.issue = Objects.requireNonNull(issue);
  }

  protected void addChildren(JsonSerializationContext context, JsonObject serialized) {
  }

  @Override
  public final JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", issue.getDoi());
    serialized.addProperty("displayName", issue.getDisplayName());

    addChildren(context, serialized);

    return serialized;
  }

  public static class DeepView extends IssueOutputView {
    private final Volume parentVolume;
    private final ImmutableList<ArticleRevisionView> articles;
    private final IssueOutputView.Factory factory;

    private DeepView(Issue issue, Volume parentVolume, List<ArticleRevisionView> articles, Factory factory) {
      super(issue);
      this.parentVolume = Objects.requireNonNull(parentVolume);
      this.factory = Objects.requireNonNull(factory);
      this.articles = ImmutableList.copyOf(articles);
    }

    @Override
    protected void addChildren(JsonSerializationContext context, JsonObject serialized) {
      serialized.add("articles", context.serialize(articles));
      serialized.add("parentVolume", context.serialize(VolumeOutputView.getView(parentVolume)));

      JsonElement imageArticle = serialized.get("imageArticle");
      if (imageArticle != null) {
        imageArticle.getAsJsonObject().addProperty("figureImageDoi", getIssueImageFigureDoi());
      }
    }

    private static final ImmutableSet<String> FIGURE_IMAGE_TYPES = ImmutableSet.of("figure", "table");

    private String getIssueImageFigureDoi() {
      Article imageArticle = issue.getImageArticle();
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

}