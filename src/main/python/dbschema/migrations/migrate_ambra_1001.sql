alter table article
  modify column doi varchar(150) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  modify column eLocationID varchar(150) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL;

alter table articleAsset
  modify column doi varchar(150) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL;

update annotation set title = replace(title, 'RE: Minor Correction: ', 'RE: Publisher''s Note: ' );
update annotation set title = replace(title, 'Minor Correction: ', '' ) where type = 'MinorCorrection';
update annotation set title = replace(title, 'Minor correction: ', '' ) where type = 'MinorCorrection';
update annotation set type = 'Comment', title = concat('Publisher''s Note: ', title) where type = 'MinorCorrection';
