package org.ambraproject.rhino.view.article;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.KeyedListView;
import org.ambraproject.rhino.view.journal.JournalNonAssocView;
import org.ambraproject.rhino.view.journal.VolumeNonAssocView;

import java.util.Collection;

public class ArticleIssueOutputView implements JsonOutputView {

  private final ArticleIssue articleIssue;

  public ArticleIssueOutputView(ArticleIssue articleIssue) {
    this.articleIssue = Preconditions.checkNotNull(articleIssue);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.add("issueUri", context.serialize(articleIssue.getIssue().getDoi()));
    serialized.add("title", context.serialize("title")); //todo: getActiveArticle & title
    serialized.add("displayName", context.serialize(articleIssue.getIssue().getDisplayName()));
    serialized.add("created", context.serialize(articleIssue.getIssue().getCreated()));
    serialized.add("lastModified", context.serialize(articleIssue.getIssue().getLastModified()));
    serialized.add("parentVolume", context.serialize(new VolumeNonAssocView(articleIssue.getParentVolume())));
    serialized.add("parentJournal", context.serialize(new JournalNonAssocView(articleIssue.getParentJournal())));

    return serialized;
  }

  public static class ListView extends KeyedListView<ArticleIssue> {
    private ListView(Collection<? extends ArticleIssue> values) {
      super(values);
    }

    @Override
    protected String getKey(ArticleIssue value) {
      return DoiBasedIdentity.asIdentifier(value.getIssue().getDoi());
    }

    @Override
    protected Object wrap(ArticleIssue value) {
      return new ArticleIssueOutputView(value);
    }
  }

  public static KeyedListView<ArticleIssue> wrapList(Collection<ArticleIssue> articleIssues) {
    return new ListView(articleIssues);
  }

}