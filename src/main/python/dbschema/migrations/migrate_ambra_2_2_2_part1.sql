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

create table userProfile (
  userProfileID bigint not null auto_increment,
  userProfileURI varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  userAccountURI varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  accountState int(11) not null,
  authId varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  realName varchar(500) CHARACTER SET utf8 COLLATE utf8_bin default null,
  givenNames varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  surName varchar(65) CHARACTER SET utf8 COLLATE utf8_bin,
  title varchar(255) CHARACTER SET utf8 COLLATE utf8_bin,
  gender varchar(15) CHARACTER SET utf8 COLLATE utf8_bin default null,
  email varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  homePage varchar(512) CHARACTER SET utf8 COLLATE utf8_bin default null,
  weblog varchar(512) CHARACTER SET utf8 COLLATE utf8_bin default null,
  publications varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  displayName varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  suffix varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  positionType varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  organizationName varchar(512) CHARACTER SET utf8 COLLATE utf8_bin default null,
  organizationType varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  organizationVisibility tinyint(1) not null DEFAULT '0',
  postalAddress text CHARACTER SET utf8 COLLATE utf8_bin,
  city varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  country varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  biography text CHARACTER SET utf8 COLLATE utf8_bin,
  interests text CHARACTER SET utf8 COLLATE utf8_bin,
  researchAreas text CHARACTER SET utf8 COLLATE utf8_bin,
  alertsJournals text CHARACTER SET utf8 COLLATE utf8_bin default null,
  created datetime not null,
  lastModified datetime not null,
  primary key (userProfileID),
  unique key (authId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table userLogin (
  userLoginID bigint not null auto_increment,
  userProfileID bigint not null,
  sessionID varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  IP varchar(100) CHARACTER SET utf8 COLLATE utf8_bin default null,
  userAgent varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  created datetime not null,
  constraint foreign key (userProfileID) references userProfile(userProfileID),
  primary key (userLoginID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table userArticleView (
  userArticleViewID bigint not null auto_increment,
  userProfileID bigint not null,
  articleID bigint not null,
  created datetime not null,
  `type` varchar(20) CHARACTER SET utf8 COLLATE utf8_bin not null,
  constraint foreign key (userProfileID) references userProfile(userProfileID),
  constraint foreign key (articleID) references article(articleID),
  primary key (userArticleViewID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table userSearch (
  userSearchID bigint not null auto_increment,
  userProfileID bigint not null,
  searchTerms text CHARACTER SET utf8 COLLATE utf8_bin default null,
  searchString text CHARACTER SET utf8 COLLATE utf8_bin default null,
  created datetime not null,
  constraint foreign key (userProfileID) references userProfile(userProfileID),
  primary key (userSearchID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table userRole (
  userRoleID bigint not null auto_increment,
  roleName varchar(15) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  created datetime not null,
  lastModified datetime not null,
  primary key (userRoleID),
  unique key (roleName)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table userProfileRoleJoinTable (
  userRoleID bigint not null,
  userProfileID bigint not null,
  primary key (userRoleID, userProfileID),
  constraint foreign key (userRoleID) references userRole(userRoleID),
  constraint foreign key (userProfileID) references userProfile(userProfileID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

