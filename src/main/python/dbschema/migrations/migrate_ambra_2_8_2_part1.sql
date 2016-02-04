create table categoryFeaturedArticle (
  categoryFeaturedArticleID bigint not null auto_increment,
  journalID bigint not null,
  articleID bigint not null,
  category varchar(100) CHARACTER SET utf8 COLLATE utf8_bin not null,
  created datetime not null,
  lastModified datetime default null,
  PRIMARY KEY (categoryFeaturedArticleID),
  constraint foreign key (journalID) references journal (journalID),
  constraint foreign key (articleID) references article (articleID),
  unique key (journalID, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

