RENAME TABLE `issue` TO `oldIssue`;

CREATE TABLE `issue` (
  `issueId` bigint(20) NOT NULL AUTO_INCREMENT,
  `issueUri` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `volumeId` bigint(20) DEFAULT NULL,
  `volumeSortOrder` int(11) DEFAULT NULL,
  `displayName` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `respectOrder` bit(1) DEFAULT NULL,
  `imageUri` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `title` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `description` text CHARACTER SET utf8 COLLATE utf8_bin,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`issueId`),
  UNIQUE KEY `issueUri` (`issueUri`),
  KEY `volumeId` (`volumeId`),
  CONSTRAINT `fk_issue_1` FOREIGN KEY (`volumeId`) REFERENCES `volume` (`volumeId`)
);

RENAME TABLE `issueArticleList` TO `oldIssueArticleList`;

CREATE TABLE `issueArticleList` (
  `issueId` bigint(20) NOT NULL DEFAULT '0',
  `sortOrder` int(11) NOT NULL DEFAULT '0',
  `articleId` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`issueId`,`sortOrder`),
  CONSTRAINT `fk_issueArticleList_1` FOREIGN KEY (`issueId`) REFERENCES `issue` (`issueId`),
  CONSTRAINT `fk_issueArticleList_2` FOREIGN KEY (`articleId`) REFERENCES `article` (`articleId`)
);
