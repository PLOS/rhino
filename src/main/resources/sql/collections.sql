/*
 * Temporary script for creating collections-related tables in development environments.
 * For the production release, we plan to convert this to an Ambra bootstrap schema migration.
 */

CREATE TABLE articleLink (
  linkID BIGINT NOT NULL AUTO_INCREMENT,
  journalID BIGINT NOT NULL,
  linkType VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  target VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  title TEXT CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,

  lastModified DATETIME NOT NULL,
  created DATETIME NOT NULL,

  PRIMARY KEY (linkID),
  UNIQUE (journalID, target),
  FOREIGN KEY (journalID) REFERENCES journal(journalID)
);

CREATE TABLE articleLinkJoinTable (
  articleID BIGINT NOT NULL,
  linkID BIGINT NOT NULL,

  sortOrder INT NOT NULL,

  PRIMARY KEY (articleID, linkID),
  FOREIGN KEY (articleID) REFERENCES article(articleID),
  FOREIGN KEY (linkID) REFERENCES articleLink(linkID)
);
