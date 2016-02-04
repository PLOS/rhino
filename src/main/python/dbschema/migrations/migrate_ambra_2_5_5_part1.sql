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
