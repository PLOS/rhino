alter table userProfile add column password varchar(255) CHARACTER SET utf8 COLLATE utf8_bin not null default 'pass';
alter table userProfile add column verificationToken varchar(255) CHARACTER SET utf8 COLLATE utf8_bin null;
alter table userProfile add column verified bit not null default true;
alter table userProfile drop column accountState;
