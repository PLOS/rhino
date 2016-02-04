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

