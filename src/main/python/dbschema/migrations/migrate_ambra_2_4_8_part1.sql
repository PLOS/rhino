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