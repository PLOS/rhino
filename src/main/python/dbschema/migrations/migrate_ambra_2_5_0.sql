update annotation set type = 'Comment' where type = 'Rating';
drop table ratingSummary;
drop table rating;

alter table annotation drop column xpath;

ALTER TABLE category ADD COLUMN path VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL AFTER subCategory;
UPDATE category SET path = CONCAT('/', mainCategory, IF(subCategory IS NULL, '', '/'), IFNULL(subCategory, ''));
ALTER TABLE category DROP KEY mainCategory;
ALTER TABLE category DROP COLUMN mainCategory;
ALTER TABLE category DROP COLUMN subCategory;
ALTER TABLE category ADD UNIQUE KEY `path` (path);