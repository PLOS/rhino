CREATE TABLE `syndication` (
  `syndicationId` bigint(20) NOT NULL AUTO_INCREMENT,
  `revisionId` BIGINT(20) NOT NULL,
  `targetQueue` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `status` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `submissionCount` int(11) DEFAULT NULL,
  `errorMessage` longtext CHARACTER SET utf8 COLLATE utf8_bin,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastSubmitTimestamp` timestamp NULL DEFAULT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`syndicationId`),
  UNIQUE KEY `revisionId` (`revisionId`,`targetQueue`),
  CONSTRAINT `fk_syndication_1`
  FOREIGN KEY (`revisionId`)
  REFERENCES `articleRevision` (`revisionId`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION
);