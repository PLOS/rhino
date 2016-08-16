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

package org.ambraproject.rhino.view.article.versioned;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Objects;

public class ArticleRevisionView implements JsonOutputView {

  private final ArticleRevision revision;

  public ArticleRevisionView(ArticleRevision revision) {
    this.revision = Objects.requireNonNull(revision);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    ArticleIngestion ingestion = revision.getIngestion();

    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", Doi.create(ingestion.getArticle().getDoi()).getName());
    serialized.addProperty("revision", revision.getRevisionNumber());
    serialized.addProperty("ingestion", ingestion.getIngestionNumber());
    return serialized;
  }

}
