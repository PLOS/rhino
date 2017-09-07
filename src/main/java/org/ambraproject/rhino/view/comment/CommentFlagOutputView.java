/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.view.comment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.user.UserIdView;

import java.util.Objects;

/**
 * Immutable View wrapper around a comment flag
 */
public class CommentFlagOutputView implements JsonOutputView {

  private final Flag flag;
  private final CommentNodeView flaggedComment;

  // Invoked from org.ambraproject.rhino.view.comment.CommentNodeView.Factory.createFlagView()
  CommentFlagOutputView(Flag flag, CommentNodeView flaggedComment) {
    this.flag = Objects.requireNonNull(flag);
    this.flaggedComment = Objects.requireNonNull(flaggedComment);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(flag).getAsJsonObject();

    // Alias "comment" (the flag field containing the comment that the user submitted) to avoid ambiguity.
    // Should match the field name used for org.ambraproject.rhino.view.comment.CommentFlagInputView.body.
    serialized.add("body", serialized.remove("comment"));

    serialized.remove("userProfileID");
    if (flag.getUserProfileId() != null) {
      serialized.add("creator", context.serialize(new UserIdView(flag.getUserProfileId())));
    }

    serialized.add("flaggedComment", context.serialize(flaggedComment));

    return serialized;
  }
}
