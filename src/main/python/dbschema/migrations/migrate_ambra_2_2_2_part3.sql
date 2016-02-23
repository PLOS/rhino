create table bad_accountURIs (userAccountUri varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL);

insert into bad_accountURIs select userAccountUri from userProfile where
  email = 'Unable%20to%20lookup%20email%20address%20from%20the%20CAS%20server.%20Please%20contact%20the%20system%20administrator.';

insert into bad_accountURIs select userAccountUri from userProfile where
  email = 'fake_guid_returned_from_cas';

insert into bad_accountURIs(userAccountUri) values('info:doi/10.1371/account/11716');

create table dupe_temp_emails
  select email from userProfile where userAccountUri not in (select userAccountUri from bad_accountURIs) group by email having count(*) > 1;

create table selected_accounts (
  userAccountUri varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  email varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  otherUserAccountUri varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NULL,
  index(userAccountUri),
  index(email)
);

insert into selected_accounts select up.userAccountUri, up.email, null
  from userProfile up join dupe_temp_emails da on da.email = up.email
   where up.displayName is not null group by email order by email;

update selected_accounts sp, userProfile up
 set sp.otherUserAccountUri = up.userAccountUri
 where sp.email = up.email and up.userAccountUri != sp.userAccountUri;

insert into bad_accountURIs select otherUserAccountUri from selected_accounts;

update Annotation a, selected_accounts s
 set a.creator = s.userAccountUri
 where a.creator = s.otherUserAccountUri;

update Reply r, selected_accounts s
 set r.creator = s.userAccountUri
 where r.creator = s.otherUserAccountUri;

delete from Annotation where creator in (select userAccountUri from bad_accountURIs);
delete from Reply where creator in (select userAccountUri from bad_accountURIs);
delete upjt from userProfileRoleJoinTable upjt inner join userProfile up on
  upjt.userProfileID = up.userProfileID
where
  up.userAccountUri in (select userAccountUri from bad_accountURIs);
delete from userProfile where userAccountUri in (select userAccountUri from bad_accountURIs);

drop table selected_accounts;
drop table bad_accountURIs;

alter table userProfile add unique key email (email);
alter table userProfile add unique key displayName (displayName);

alter table userProfile add unique key userAccountURI (userAccountURI);
alter table userProfile add unique key userProfileURI (userProfileURI);

alter table Annotation drop foreign key Annotation_ibfk_2;
alter table Annotation add foreign key (creator) references userProfile(userAccountUri);

alter table Reply drop foreign key Reply_ibfk_1;
alter table Reply add foreign key (creator) references userProfile(userAccountUri);

