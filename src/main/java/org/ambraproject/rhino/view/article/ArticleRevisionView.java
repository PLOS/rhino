/*
 * Copyright (c) 2016 PLOS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.ambraproject.rhino.view.article;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.journal.JournalOutputView;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.Optional;

public class ArticleRevisionView implements JsonOutputView {

  public static class Factory {
    @Autowired
    private ArticleCrudService articleCrudService;

    /**
     * @param article the article to represent
     * @return a view representing the article and, if it has one, its latest revision
     */
    public ArticleRevisionView getLatestRevisionView(Article article) {
      return new ArticleRevisionView(article, articleCrudService.getLatestRevision(article));
    }
  }

  public static ArticleRevisionView getView(ArticleRevision revision) {
    return new ArticleRevisionView(revision.getIngestion().getArticle(), Optional.of(revision));
  }


  private final Article article;
  private final Optional<ArticleRevision> revision;

  private ArticleRevisionView(Article article, Optional<ArticleRevision> revision) {
    this.article = Objects.requireNonNull(article);
    this.revision = Objects.requireNonNull(revision);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", article.getDoi());

    this.revision.ifPresent((ArticleRevision revision) -> {
      serialized.addProperty("revisionNumber", revision.getRevisionNumber());
      serialized.add("ingestion", serializeIngestion(context, revision.getIngestion()));
    });

    return serialized;
  }

  // Could be extracted for more public use, in case we ever need this shallow ArticleIngestion view elsewhere.
  private JsonObject serializeIngestion(JsonSerializationContext context, ArticleIngestion ingestion) {
    JsonObject serialized = new JsonObject();
    JournalOutputView journalOutputView = JournalOutputView.getShallowView(ingestion.getJournal());
    serialized.add("journal", context.serialize(journalOutputView));
    serialized.addProperty("ingestionNumber", ingestion.getIngestionNumber());
    serialized.addProperty("title", ingestion.getTitle());
    serialized.addProperty("publicationDate", ingestion.getPublicationDate().toLocalDate().toString());
    if (ingestion.getRevisionDate() != null) {
      serialized.addProperty("revisionDate", ingestion.getRevisionDate().toLocalDate().toString());
    }
    serialized.addProperty("articleType", ingestion.getArticleType());
    return serialized;
  }

}
