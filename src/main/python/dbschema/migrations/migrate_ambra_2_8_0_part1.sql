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

create table articleList (
  articleListID bigint not null auto_increment,
  listCode varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  displayName varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  journalID bigint default null,
  journalSortOrder int(11) default null,
  created datetime not null,
  lastModified datetime not null,
  PRIMARY KEY (articleListID),
  constraint unique key (listCode),
  constraint foreign key (journalID) references journal (journalID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table articleListJoinTable (
  articleListID bigint not null,
  sortOrder int(11) not null,
  doi varchar(255) CHARACTER SET utf8 COLLATE utf8_bin null,
  PRIMARY KEY (articleListID, sortOrder),
  constraint foreign key (articleListID) references articleList (articleListID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table articleCategoryFlagged (
  articleID bigint not null,
  categoryID bigint not null,
  userProfileID bigint null,
  created datetime not null,
  lastModified datetime not null,
  constraint foreign key (articleID) references article (articleID),
  constraint foreign key (categoryID) references category (categoryID),
  constraint foreign key (userProfileID) references userProfile (userProfileID),
  UNIQUE KEY (articleID, categoryID, userProfileID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

