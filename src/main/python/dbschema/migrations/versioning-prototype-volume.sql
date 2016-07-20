RENAME TABLE `volume` TO `oldVolume`;

CREATE TABLE `volume` (
  `volumeId` bigint(20) NOT NULL AUTO_INCREMENT,
  `doi` varchar(150) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `journalId` bigint(20) DEFAULT NULL,
  `journalSortOrder` int(11) DEFAULT NULL,
  `displayName` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `imageArticleId` bigint(20) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`volumeId`),
  UNIQUE KEY `doi` (`doi`),
  KEY `journalID` (`journalId`),
  CONSTRAINT `fk_volume_1` FOREIGN KEY (`journalId`) REFERENCES `journal` (`journalID`),
  CONSTRAINT `fk_volume_2` FOREIGN KEY (`imageArticleId`) REFERENCES `article` (`articleId`)
);
