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

ALTER TABLE article ADD COLUMN strkImgURI VARCHAR(50) CHARACTER SET utf8 COLLATE utf8_bin null after url;
ALTER TABLE annotation ADD COLUMN highlightedText TEXT CHARACTER SET utf8 COLLATE utf8_bin null after competingInterestBody;
ALTER TABLE pingback change column url url varchar(255) CHARACTER SET utf8 COLLATE utf8_bin not null,
  change column title title varchar(255) CHARACTER SET utf8 COLLATE utf8_bin null,
  change column created created datetime not null after title,
  change column lastModified lastModified datetime null after created;

  