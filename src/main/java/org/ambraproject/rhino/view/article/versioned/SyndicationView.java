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
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Objects;

public class SyndicationView implements JsonOutputView {

  private final Syndication syndication;

  public SyndicationView(Syndication syndication) {
    this.syndication = Objects.requireNonNull(syndication);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.add("revision", context.serialize(ArticleRevisionView.getView(syndication.getArticleRevision())));
    serialized.addProperty("target", syndication.getTarget());
    serialized.addProperty("status", syndication.getStatus());
    serialized.addProperty("submissionCount", syndication.getSubmissionCount());
    serialized.add("lastSubmitTimestamp", context.serialize(syndication.getLastSubmitTimestamp()));
    return serialized;
  }

}
