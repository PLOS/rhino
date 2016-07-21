RENAME TABLE `issue` TO `oldIssue`;

CREATE TABLE `issue` (
  `issueId` bigint(20) NOT NULL AUTO_INCREMENT,
  `doi` varchar(150) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `volumeId` bigint(20) DEFAULT NULL,
  `volumeSortOrder` int(11) DEFAULT NULL,
  `displayName` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `imageArticleId` bigint(20) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`issueId`),
  UNIQUE KEY `doi` (`doi`),
  KEY `volumeId` (`volumeId`),
  CONSTRAINT `fk_issue_1` FOREIGN KEY (`volumeId`) REFERENCES `volume` (`volumeId`),
  CONSTRAINT `fk_issue_2` FOREIGN KEY (`imageArticleId`) REFERENCES `article` (`articleId`)
);

RENAME TABLE `issueArticleList` TO `oldIssueArticleList`;

CREATE TABLE `issueArticleList` (
  `issueId` bigint(20) NOT NULL DEFAULT '0',
  `sortOrder` int(11) NOT NULL '0',
  `articleId` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`issueId`,`articleId`),
  CONSTRAINT `fk_issueArticleList_1` FOREIGN KEY (`issueId`) REFERENCES `issue` (`issueId`),
  CONSTRAINT `fk_issueArticleList_2` FOREIGN KEY (`articleId`) REFERENCES `article` (`articleId`)
);
