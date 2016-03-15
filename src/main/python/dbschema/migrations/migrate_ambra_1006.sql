CREATE TABLE doiAssociation (
  doiAssociationID BIGINT NOT NULL AUTO_INCREMENT,
  doi VARCHAR(150) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  parentArticleDoi VARCHAR(150) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,

  created DATETIME NOT NULL,
  lastModified DATETIME NOT NULL,

  PRIMARY KEY(doiAssociationId),
  UNIQUE KEY(doi)
);
