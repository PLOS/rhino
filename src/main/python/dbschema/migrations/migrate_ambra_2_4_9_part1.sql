ALTER TABLE article ADD COLUMN strkImgURI VARCHAR(50) CHARACTER SET utf8 COLLATE utf8_bin null after url;
ALTER TABLE annotation ADD COLUMN highlightedText TEXT CHARACTER SET utf8 COLLATE utf8_bin null after competingInterestBody;
ALTER TABLE pingback change column url url varchar(255) CHARACTER SET utf8 COLLATE utf8_bin not null,
  change column title title varchar(255) CHARACTER SET utf8 COLLATE utf8_bin null,
  change column created created datetime not null after title,
  change column lastModified lastModified datetime null after created;

  