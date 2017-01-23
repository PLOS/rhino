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

create table savedSearchQuery (
  savedSearchQueryID bigint(20) not null auto_increment,
  searchParams text character set utf8 collate utf8_bin not null,
  hash varchar(50) character set utf8 collate utf8_bin null,
  created datetime not null,
  lastmodified datetime not null,
  primary key (savedSearchQueryID)
) engine=innodb auto_increment=1 default charset=utf8;

alter table savedSearch
  add column savedSearchQueryID bigint(20) null after userProfileID,
  add column searchType varchar(16) not null default 'User Defined' after searchName,
  add constraint foreign key (savedSearchQueryID) references savedSearchQuery(savedSearchQueryID);

insert into savedSearchQuery(searchParams, created, lastmodified)
  select distinct searchParams, now(), now() from savedSearch;

update savedSearch, savedSearchQuery
  set savedSearch.savedSearchQueryID = savedSearchQuery.savedSearchQueryID
  where savedSearch.searchParams = savedSearchQuery.searchParams;

alter table savedSearch
  modify column savedSearchQueryID bigint(20) not null,
  modify column searchType varchar(16) not null,
  drop column searchParams;
