RENAME TABLE `articleList` TO `oldArticleList`;

RENAME TABLE `articleListJoinTable` TO `oldArticleListJoinTable`;

CREATE TABLE `articleList` (
  `articleListId` bigint(20) NOT NULL AUTO_INCREMENT,
  `listKey` varchar(255) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `displayName` varchar(255) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NULL,
  `journalId` bigint(20) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `listType` varchar(255) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  PRIMARY KEY (`articleListId`),
  UNIQUE KEY `listIdentity` (`journalId`,`listType`,`listKey`),
  KEY `journalId` (`journalId`),
  CONSTRAINT `fk_articleList_1` FOREIGN KEY (`journalId`) REFERENCES `journal` (`journalID`)
);

CREATE TABLE `articleListJoinTable` (
  `articleListId` bigint(20) NOT NULL,
  `sortOrder` int(11) NOT NULL,
  `articleId` bigint(20) NOT NULL,
  PRIMARY KEY (`articleListId`,`sortOrder`),
  KEY `articleId` (`articleId`),
  CONSTRAINT `fk_articleListJoinTable_1` FOREIGN KEY (`articleListId`) REFERENCES `articleList` (`articleListId`),
  CONSTRAINT `fk_articleListJoinTable_2` FOREIGN KEY (`articleId`) REFERENCES `article` (`articleId`)
);