RENAME TABLE `articleList` TO `oldArticleList`;

RENAME TABLE `articleListJoinTable` TO `oldArticleListJoinTable`;

CREATE TABLE `articleList` (
  `articleListID` bigint(20) NOT NULL AUTO_INCREMENT,
  `listKey` varchar(255) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `displayName` varchar(255) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL,
  `journalID` bigint(20) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `listType` varchar(255) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  PRIMARY KEY (`articleListID`),
  UNIQUE KEY `listIdentity` (`journalID`,`listType`,`listKey`),
  KEY `journalID` (`journalID`),
  CONSTRAINT `fk_articleList_1` FOREIGN KEY (`journalID`) REFERENCES `journal` (`journalID`)
);

CREATE TABLE `articleListJoinTable` (
  `articleListID` bigint(20) NOT NULL,
  `sortOrder` int(11) NOT NULL,
  `articleID` bigint(20) NOT NULL,
  PRIMARY KEY (`articleListID`,`sortOrder`),
  KEY `articleID` (`articleID`),
  CONSTRAINT `fk_articleListJoinTable_1` FOREIGN KEY (`articleListID`) REFERENCES `articleList` (`articleListID`),
  CONSTRAINT `fk_articleListJoinTable_2` FOREIGN KEY (`articleID`) REFERENCES `article` (`articleID`)
);