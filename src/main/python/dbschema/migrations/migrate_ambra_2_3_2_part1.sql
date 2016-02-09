alter table Annotation drop foreign key Annotation_ibfk_1, drop foreign key Annotation_ibfk_2;
alter table Trackback drop foreign key Trackback_ibfk_1;

create table annotationCitation (
  annotationCitationID bigint not null auto_increment,
  year varchar(255) character set utf8 collate utf8_bin default null,
  volume varchar(255) character set utf8 collate utf8_bin default null,
  issue varchar(255) character set utf8 collate utf8_bin default null,
  journal varchar(255) character set utf8 collate utf8_bin default null,
  title text character set utf8 collate utf8_bin default null,
  publisherName text character set utf8 collate utf8_bin default null,
  eLocationId varchar(255) character set utf8 collate utf8_bin default null,
  note text character set utf8 collate utf8_bin default null,
  url varchar(255) character set utf8 collate utf8_bin default null,
  summary varchar(10000) character set utf8 collate utf8_bin default null,
  created datetime not null,
  lastModified datetime not null,
  primary key (annotationCitationID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table annotation (
  annotationID bigint not null auto_increment,
  annotationURI varchar(255) character set utf8 collate utf8_bin not null,
  articleID bigint null,
  parentID bigint null,
  userProfileID bigint not null,
  annotationCitationID bigint,
  type varchar(16) character set utf8 collate utf8_bin default null,
  title text character set utf8 collate utf8_bin default null,
  body text character set utf8 collate utf8_bin default null,
  xpath text character set utf8 collate utf8_bin default null,
  competingInterestBody text character set utf8 collate utf8_bin default null,
  created datetime not null,
  lastModified datetime not null,
  primary key (annotationID),
  constraint unique key (annotationURI),
  constraint unique key (annotationCitationID),
  constraint foreign key (articleID) references article(articleID),
  constraint foreign key (annotationCitationID) references annotationCitation(annotationCitationID),
  constraint foreign key (parentID) references annotation(annotationID),
  constraint foreign key (userProfileID) references userProfile(userProfileID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table annotationFlag (
  annotationFlagID bigint not null auto_increment,
  annotationID bigint not null,
  userProfileID bigint not null,
  reason varchar(25) character set utf8 collate utf8_bin not null,
  comment text character set utf8 collate utf8_bin default null,
  created datetime not null,
  lastModified datetime not null,
  primary key (annotationFlagID),
  constraint foreign key (annotationID) references annotation(annotationID),
  constraint foreign key (userProfileID) references userProfile(userProfileID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table annotationCitationAuthor (
  annotationCitationAuthorID bigint not null auto_increment,
  annotationCitationID bigint null,
  fullName varchar(200) character set utf8 collate utf8_bin null,
  givenNames varchar(150) character set utf8 collate utf8_bin null,
  surnames varchar(200) character set utf8 collate utf8_bin null,
  suffix varchar(100) character set utf8 collate utf8_bin null,
  sortOrder integer null,
  created datetime not null,
  lastModified datetime not null,
  constraint foreign key (annotationCitationID) references annotationCitation(annotationCitationID),
  primary key (annotationCitationAuthorID),
  unique key (annotationCitationID, sortOrder)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table annotationCitationCollabAuthor (
  annotationCitationID bigint null,
  sortOrder integer,
  name varchar(255) character set utf8 collate utf8_bin null,
  primary key (annotationCitationID, sortOrder),
  constraint foreign key (annotationCitationID) references annotationCitation(annotationCitationID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table rating (
  annotationID bigint not null,
  insight int not null,
  reliability int not null,
  style int not null,
  singleRating int not null,
  primary key (annotationID),
  constraint foreign key (annotationID) references annotation(annotationID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table ratingSummary (
  ratingSummaryID bigint not null auto_increment,
  articleID bigint not null,
  insightNumRatings int not null,
  insightTotal int not null,
  reliabilityNumRatings int not null,
  reliabilityTotal int not null,
  styleNumRatings int not null,
  styleTotal int not null,
  singleRatingNumRatings int not null,
  singleRatingTotal int not null,
  usersThatRated int not null,
  created datetime not null,
  lastModified datetime not null,
  primary key (ratingSummaryID),
  constraint foreign key (articleID) references article(articleID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table trackback (
  trackbackID bigint not null auto_increment,
  articleID bigint not null,
  url varchar(500) character set utf8 collate utf8_bin not null,
  title varchar(500) character set utf8 collate utf8_bin null,
  blogname varchar(500) character set utf8 collate utf8_bin not null,
  excerpt text character set utf8 collate utf8_bin not null,
  created datetime not null,
  lastModified datetime not null,
  PRIMARY KEY (trackbackID),
  constraint foreign key (articleID) references article(articleID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;





