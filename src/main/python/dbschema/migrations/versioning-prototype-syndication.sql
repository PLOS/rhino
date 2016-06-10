RENAME TABLE `syndication` TO `oldSyndication`;

CREATE TABLE `syndication` (
  `syndicationId` bigint(20) NOT NULL AUTO_INCREMENT,
  `articleVersionId` BIGINT(20) NOT NULL,
  `target` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `status` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `submissionCount` int(11) DEFAULT NULL,
  `errorMessage` longtext CHARACTER SET utf8 COLLATE utf8_bin,
  `created` datetime NOT NULL,
  `lastSubmitTimestamp` datetime DEFAULT NULL,
  `lastModified` datetime DEFAULT NULL,
  PRIMARY KEY (`syndicationId`),
  UNIQUE KEY `articleVersionId` (`articleVersionId`,`target`),
  CONSTRAINT `fk_syndication_1`
  FOREIGN KEY (`articleVersionID`)
  REFERENCES `ambra`.`articleVersion` (`versionId`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION
);