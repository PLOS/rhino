package org.ambraproject.rhino.view.journal;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Issue;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.KeyedListView;
import org.ambraproject.rhino.view.article.DoiList;

import java.util.Collection;
import java.util.List;

public class IssueOutputView implements JsonOutputView {

  private final Issue issue;

  public IssueOutputView(Issue issue) {
    this.issue = Preconditions.checkNotNull(issue);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(issue).getAsJsonObject();

    serialized.remove("articleDois");
    List<String> articleDois = issue.getArticleDois();
    DoiList articlesView = new DoiList(articleDois);
    serialized.add("articles", context.serialize(articlesView));

    return serialized;
  }

  private static final Function<Issue, IssueOutputView> WRAP = new Function<Issue, IssueOutputView>() {
    @Override
    public IssueOutputView apply(Issue input) {
      return new IssueOutputView(input);
    }
  };

  public static class ListView extends KeyedListView<IssueOutputView> {
    private ListView(Collection<? extends IssueOutputView> values) {
      super(values);
    }

    @Override
    protected String getKey(IssueOutputView value) {
      return DoiBasedIdentity.asIdentifier(value.issue.getIssueUri());
    }
  }

  public static KeyedListView<IssueOutputView> wrapList(Collection<Issue> issues) {
    Collection<IssueOutputView> viewList = Collections2.transform(issues, WRAP);
    return new ListView(viewList);
  }

}