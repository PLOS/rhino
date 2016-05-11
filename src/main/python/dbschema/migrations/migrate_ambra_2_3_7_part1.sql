create table userRolePermission (
  userRoleID bigint not null,
  permission varchar(255) CHARACTER SET utf8 COLLATE utf8_bin default null,
  primary key (userRoleID, permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table userRolePermission add constraint foreign key (userRoleID) references userRole (userRoleID);
