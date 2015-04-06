/*
 * Add new tables that are private to Rhino (i.e., not used by legacy Ambra)
 * for the new versioned-article system.
 *
 * This script is meant to be run by hand. It is a temporary kludge while the
 * versioned-article system is under development, and will need to be
 * integrated with Ambra's "bootstrap migration" system (and properly record
 * itself in the Version table) when it becomes part of a permanent feature.
 */

CREATE TABLE articleRevision (
  articleRevisionID BIGINT NOT NULL AUTO_INCREMENT,
  doi VARCHAR(150) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  revisionNumber INTEGER NOT NULL,
  crepoUuid CHAR(36) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,

  created DATETIME NOT NULL,
  lastModified DATETIME NOT NULL,

  PRIMARY KEY(articleRevisionId),
  UNIQUE KEY(doi, revisionNumber)
);

CREATE TABLE articleAssociation (
  articleAssociationID BIGINT NOT NULL AUTO_INCREMENT,
  doi VARCHAR(150) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  parentArticleDoi VARCHAR(150) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,

  created DATETIME NOT NULL,
  lastModified DATETIME NOT NULL,

  PRIMARY KEY(articleAssociationId),
  UNIQUE KEY(doi)
);
