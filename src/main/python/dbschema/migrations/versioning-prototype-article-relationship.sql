RENAME TABLE `articleRelationship` TO `oldArticleRelationship`;

CREATE TABLE `articleRelationship` (
  `articleRelationshipId` bigint(20) NOT NULL AUTO_INCREMENT,
  `sourceArticleId` bigint(20) NOT NULL,
  `targetArticleId` bigint(20) NOT NULL,
  `type` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`articleRelationshipId`),
  KEY `sourceArticleId` (`sourceArticleId`),
  KEY `targetArticleId` (`targetArticleId`),
  CONSTRAINT `fk_articleRelationship_1` FOREIGN KEY (`sourceArticleId`) REFERENCES `article` (`articleId`),
  CONSTRAINT `fk_articleRelationship_2` FOREIGN KEY (`targetArticleId`) REFERENCES `article` (`articleId`)
);