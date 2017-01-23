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

package org.ambraproject.rhino.view.user;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.rhino.view.JsonOutputView;

/**
 * Wrapper for an ID pointing into the user database.
 */
public class UserIdView implements JsonOutputView {

  private final long userProfileId;

  public UserIdView(long userProfileId) {
    this.userProfileId = userProfileId;
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();

    /*
     * Although the userProfileId's low-level representation is a long integer, in the API we expose this essentially
     * non-numeric value as a string. This serves two purposes. First, it is general future-proofing in case we ever
     * want to change the ID scheme to allow arbitrary strings. Second, it helps prevent loss of precision in case the
     * client deserializes it to a floating-point value or smaller integer.
     */
    String userId = String.valueOf(userProfileId);

    serialized.addProperty("userId", userId);
    return serialized;
  }

  @Override
  public boolean equals(Object o) {
    return (this == o) || ((o != null) && (getClass() == o.getClass())
        && (userProfileId == ((UserIdView) o).userProfileId));
  }

  @Override
  public int hashCode() {
    return Long.hashCode(userProfileId);
  }

}
