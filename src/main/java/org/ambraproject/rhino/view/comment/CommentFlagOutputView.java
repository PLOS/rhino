/* $HeadURL::                                                                            $
 * $Id$
 *
 * Copyright (c) 2006-2010 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    serialized.add("creator", context.serialize(new UserIdView(flag.getUserProfileId())));

    serialized.remove("flaggedAnnotation");
    serialized.add("flaggedComment", context.serialize(flaggedComment));

    return serialized;
  }
}
