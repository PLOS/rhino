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

alter table IssueArticleList drop foreign key IssueArticleList_ibfk_1;
alter table VolumeIssueList drop foreign key VolumeIssueList_ibfk_1;
alter table JournalVolumeList drop foreign key JournalVolumeList_ibfk_1;

create table journal (
  journalID bigint not null auto_increment,
  currentIssueID bigint null,
  currentIssueUri varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  journalKey varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  eIssn varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  imageUri varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  title varchar(500) CHARACTER SET utf8 COLLATE utf8_bin default null,
  description text CHARACTER SET utf8 COLLATE utf8_bin default null,
  created datetime not null,
  lastModified datetime not null,
  primary key (journalID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table volume (
  volumeID bigint not null auto_increment,
  volumeUri varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  journalID bigint default null,
  journalSortOrder int(11) default null,
  displayName varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  imageUri varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  title varchar(500) CHARACTER SET utf8 COLLATE utf8_bin default null,
  description text CHARACTER SET utf8 COLLATE utf8_bin default null,
  created datetime not null,
  lastModified datetime not null,
  primary key (volumeID),
  constraint unique key (volumeUri),
  constraint foreign key (journalID) references journal (journalID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table issue (
  issueID bigint not null auto_increment,
  issueUri varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  volumeID bigint default null,
  volumeSortOrder int(11) default NULL,
  displayName varchar(255) CHARACTER SET utf8 COLLATE utf8_bin not null,
  respectOrder bit(1) default null,
  imageUri varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  title varchar(500) CHARACTER SET utf8 COLLATE utf8_bin default null,
  description text CHARACTER SET utf8 COLLATE utf8_bin default null,
  created datetime not null,
  lastModified datetime not null,
  PRIMARY KEY (issueID),
  constraint unique key (issueUri),
  constraint foreign key (volumeID) references volume (volumeID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table issueArticleList (
  issueID bigint default null,
  sortOrder int(11) default null,
  doi varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  primary key (issueID, sortOrder),
  constraint foreign key (issueID) references issue (issueID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table articlePublishedJournals (
  articleID bigint not null,
  journalID bigint not null,
  primary key (articleID, journalID),
  constraint foreign key (articleID) references article (articleID),
  constraint foreign key (journalID) references journal (journalID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table journal add constraint foreign key (currentIssueID) references issue (issueID);