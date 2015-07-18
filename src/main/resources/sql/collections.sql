/*
 * Temporary script for creating collections-related tables in development environments.
 * For the production release, we plan to convert this to an Ambra bootstrap schema migration.
 */

CREATE TABLE articleCollection (
  collectionID BIGINT NOT NULL AUTO_INCREMENT,
  journalID BIGINT NOT NULL,
  slug VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  title TEXT CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,

  lastModified DATETIME NOT NULL,
  created DATETIME NOT NULL,

  PRIMARY KEY (collectionID),
  UNIQUE (journalID, slug),
  FOREIGN KEY (journalID) REFERENCES journal(journalID)
);

CREATE TABLE articleCollectionJoinTable (
  articleID BIGINT NOT NULL,
  collectionID BIGINT NOT NULL,

  sortOrder INT NOT NULL,

  PRIMARY KEY (articleID, collectionID),
  FOREIGN KEY (articleID) REFERENCES article(articleID),
  FOREIGN KEY (collectionID) REFERENCES articleCollection(collectionID)
);
