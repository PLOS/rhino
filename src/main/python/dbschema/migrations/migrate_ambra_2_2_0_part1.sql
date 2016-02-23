create table `version`(
  versionID bigint not null auto_increment,
  name varchar(25) CHARACTER SET utf8 COLLATE utf8_bin not null,
  version int not null,
  updateInProcess bit not null,
  created datetime not null,
  lastModified datetime null,
  primary key (versionID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table article (
  articleID bigint not null auto_increment,
  doi varchar(50) CHARACTER SET utf8 COLLATE utf8_bin null,
  title varchar(500) CHARACTER SET utf8 COLLATE utf8_bin null,
  eIssn varchar(15) CHARACTER SET utf8 COLLATE utf8_bin null,
  state integer not null,
  archiveName varchar(50) CHARACTER SET utf8 COLLATE utf8_bin null,
  description text CHARACTER SET utf8 COLLATE utf8_bin null,
  rights text CHARACTER SET utf8 COLLATE utf8_bin null,
  language varchar(5) CHARACTER SET utf8 COLLATE utf8_bin null,
  format varchar(10) CHARACTER SET utf8 COLLATE utf8_bin null,
  date datetime not null,
  volume varchar(5) CHARACTER SET utf8 COLLATE utf8_bin null,
  issue varchar(5) CHARACTER SET utf8 COLLATE utf8_bin null,
  journal varchar(50) CHARACTER SET utf8 COLLATE utf8_bin null,
  publisherLocation varchar(25) CHARACTER SET utf8 COLLATE utf8_bin null,
  publisherName varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  pages varchar(15) CHARACTER SET utf8 COLLATE utf8_bin null,
  eLocationID varchar(15) CHARACTER SET utf8 COLLATE utf8_bin null,
  url varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  created datetime not null,
  lastModified datetime null,
  index(doi),
  primary key (articleID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table articleAsset (
  articleAssetID bigint not null auto_increment,
  articleID bigint null,
  doi varchar(75) CHARACTER SET utf8 COLLATE utf8_bin not null,
  contextElement varchar(30) CHARACTER SET utf8 COLLATE utf8_bin null,
  contentType varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  extension varchar(10) CHARACTER SET utf8 COLLATE utf8_bin null,
  title varchar(500) CHARACTER SET utf8 COLLATE utf8_bin null,
  description text CHARACTER SET utf8 COLLATE utf8_bin null,
  size bigint,
  sortOrder integer null,
  created datetime not null,
  lastModified datetime null,
  constraint foreign key (articleID) references article(articleID),
  index (articleID),
  index (doi),
  unique key(doi, extension),
  primary key (articleAssetID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table category (
  categoryID bigint not null auto_increment,
  mainCategory varchar(50) CHARACTER SET utf8 COLLATE utf8_bin null,
  subCategory varchar(150) CHARACTER SET utf8 COLLATE utf8_bin null,
  created datetime,
  lastModified datetime,
  primary key (categoryID),
  unique key (mainCategory, subCategory)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table articleCategoryJoinTable (
  articleID bigint not null,
  categoryID bigint not null,
  index(articleID),
  index(categoryID),
  primary key (articleID, categoryID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table articleCollaborativeAuthors (
  articleID bigint not null,
  sortOrder integer not null,
  name varchar(255) CHARACTER SET utf8 COLLATE utf8_bin null,
  index(articleID),
  primary key (articleID, sortOrder)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table articlePerson (
  articlePersonID bigint not null auto_increment,
  articleID bigint null,
  sortOrder integer,
  type varchar(15) CHARACTER SET utf8 COLLATE utf8_bin not null,
  fullName varchar(100) CHARACTER SET utf8 COLLATE utf8_bin not null,
  givenNames varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  surnames varchar(100) CHARACTER SET utf8 COLLATE utf8_bin not null,
  suffix varchar(15) CHARACTER SET utf8 COLLATE utf8_bin null,
  created datetime not null,
  lastModified datetime null,
  constraint foreign key (articleID) references article(articleID),
  index(articleID),
  primary key (articlePersonID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table articleRelationship (
  articleRelationshipID bigint not null auto_increment,
  parentArticleID bigint not null,
  otherArticleDoi varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  otherArticleID bigint null,
  type varchar(50) CHARACTER SET utf8 COLLATE utf8_bin not null,
  sortOrder integer not null,
  created datetime not null,
  lastModified datetime null,
  constraint foreign key (parentArticleID) references article(articleID),
  constraint foreign key (otherArticleID) references article(articleID),
  index(parentArticleID),
  index(otherArticleID),
  primary key (articleRelationshipID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table articleType (
  articleID bigint not null,
  type varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  constraint foreign key (articleID) references article(articleID),
  index(articleID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table citedArticle (
  citedArticleID bigint not null auto_increment,
  uri varchar(150) CHARACTER SET utf8 COLLATE utf8_bin null,
  articleID bigint null,
  keyColumn varchar(10) CHARACTER SET utf8 COLLATE utf8_bin null,
  year integer null,
  displayYear varchar(50) CHARACTER SET utf8 COLLATE utf8_bin null,
  month varchar(15) CHARACTER SET utf8 COLLATE utf8_bin null,
  day varchar(20) CHARACTER SET utf8 COLLATE utf8_bin null,
  volumeNumber integer null,
  volume varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  issue varchar(60) CHARACTER SET utf8 COLLATE utf8_bin null,
  title text CHARACTER SET utf8 COLLATE utf8_bin null,
  publisherLocation varchar(250) CHARACTER SET utf8 COLLATE utf8_bin null,
  publisherName text CHARACTER SET utf8 COLLATE utf8_bin null,
  pages varchar(150) CHARACTER SET utf8 COLLATE utf8_bin null,
  eLocationID varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  journal varchar(250) CHARACTER SET utf8 COLLATE utf8_bin null,
  note text CHARACTER SET utf8 COLLATE utf8_bin null,
  url varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  doi varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  citationType varchar(60) CHARACTER SET utf8 COLLATE utf8_bin null,
  summary varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  sortOrder integer null,
  created datetime not null,
  lastModified datetime null,
  constraint foreign key (articleID) references article(articleID),
  index(articleID),
  primary key (citedArticleID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table citedArticleCollaborativeAuthors (
  citedArticleID bigint not null,
  sortOrder integer not null,
  name varchar(200) CHARACTER SET utf8 COLLATE utf8_bin null,
  constraint foreign key (citedArticleID) references citedArticle(citedArticleID),
  index(citedArticleID),
  primary key (citedArticleID, sortOrder)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table citedPerson (
  citedPersonID bigint not null auto_increment,
  citedArticleID bigint null,
  type varchar(15) CHARACTER SET utf8 COLLATE utf8_bin not null,
  fullName varchar(200) CHARACTER SET utf8 COLLATE utf8_bin null,
  givenNames varchar(150) CHARACTER SET utf8 COLLATE utf8_bin null,
  surnames varchar(200) CHARACTER SET utf8 COLLATE utf8_bin null,
  suffix varchar(100) CHARACTER SET utf8 COLLATE utf8_bin null,
  sortOrder integer null,
  created datetime not null,
  lastModified datetime null,
  constraint foreign key (citedArticleID) references citedArticle(citedArticleID),
  index(citedArticleID),
  primary key (citedPersonID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table syndication (
  syndicationID bigint not null auto_increment,
  doi varchar(255) CHARACTER SET utf8 COLLATE utf8_bin not null,
  target varchar(50) CHARACTER SET utf8 COLLATE utf8_bin not null,
  status varchar(50) CHARACTER SET utf8 COLLATE utf8_bin not null,
  submissionCount integer,
  errorMessage longtext CHARACTER SET utf8 COLLATE utf8_bin null,
  created datetime not null,
  lastSubmitTimestamp datetime null,
  lastModified datetime null,
  primary key (syndicationID),
  unique (doi, target)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;






