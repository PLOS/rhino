create table userOrcid (
  userProfileID bigint not null,
  orcid varchar(25) character set utf8 collate utf8_bin not null,
  accessToken varchar(50) character set utf8 collate utf8_bin not null,
  refreshToken varchar(50) character set utf8 collate utf8_bin not null,
  tokenScope varchar(100) character set utf8 collate utf8_bin not null,
  tokenExpires datetime not null,
  lastModified datetime not null,
  created datetime not null,
  primary key (userProfileID),
  constraint foreign key (userProfileID) references userProfile (userProfileID),
  unique (orcid)
);

