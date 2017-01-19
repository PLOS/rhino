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

create table savedSearch (
  savedSearchID bigint not null auto_increment,
  userProfileID bigint not null,
  searchName varchar(255) character set utf8 collate utf8_bin not null,
  searchParams text CHARACTER SET utf8 COLLATE utf8_bin not null,
  lastWeeklySearchTime datetime not null,
  lastMonthlySearchTime datetime not null,
  monthly bit default false,
  weekly bit default false,
  created datetime not null,
  lastModified datetime not null,
  constraint foreign key (userProfileID) references userProfile(userProfileID),
  primary key (savedSearchID),
  unique key (userProfileID, searchName)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;