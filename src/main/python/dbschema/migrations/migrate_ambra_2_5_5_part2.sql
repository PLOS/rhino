alter table savedSearchQuery
  modify column hash varchar(50) character set utf8 collate utf8_bin not null,
  add index (hash),
  add unique key(hash);
