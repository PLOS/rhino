
CREATE TABLE `comment` (
  `commentId` bigint(20) NOT NULL AUTO_INCREMENT,
  `commentURI` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `articleId` bigint(20) DEFAULT NULL,
  `parentId` bigint(20) DEFAULT NULL,
  `userProfileId` bigint(20) NOT NULL,
  `type` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `title` text CHARACTER SET utf8 COLLATE utf8_bin,
  `body` text CHARACTER SET utf8 COLLATE utf8_bin,
  `competingInterestBody` text CHARACTER SET utf8 COLLATE utf8_bin,
  `highlightedText` text CHARACTER SET utf8 COLLATE utf8_bin,
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  `isRemoved` bit(1) DEFAULT b'0',
  PRIMARY KEY (`commentId`),
  UNIQUE KEY `commentURI` (`commentURI`),
  CONSTRAINT `fk_comment_1` FOREIGN KEY (`articleId`) REFERENCES `ambra`.`article` (`articleId`),
  CONSTRAINT `fk_comment_2` FOREIGN KEY (`parentId`)  REFERENCES `ambra`.`comment` (`commentId`),
  KEY `userProfileID` (`userProfileID`)
)

CREATE TABLE `commentFlag` (
  `commentFlagId` bigint(20) NOT NULL AUTO_INCREMENT,
  `commentId` bigint(20) NOT NULL,
  `userProfileId` bigint(20) NOT NULL,
  `reason` varchar(25) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `comment` text CHARACTER SET utf8 COLLATE utf8_bin,
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  PRIMARY KEY (`commentFlagId`),
  KEY `commentId` (`commentId`),
  KEY `userProfileId` (`userProfileId`),
  CONSTRAINT `fk_commentFlag_1` FOREIGN KEY (`commentId`) REFERENCES `comment` (`commentId`)
)