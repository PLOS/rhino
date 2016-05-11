create table userProfileMetaData (
  userProfileMetaDataID bigint not null auto_increment,
  userProfileID bigint not null,
  metaKey varchar(50) character set utf8 collate utf8_bin not null,
  metaValue varchar(255) character set utf8 collate utf8_bin,
  lastModified datetime not null,
  created datetime not null,
  primary key (userProfileMetaDataID),
  constraint foreign key (userProfileID) references userProfile (userProfileID),
  index(metaKey),
  unique (userProfileID, metaKey)
);
