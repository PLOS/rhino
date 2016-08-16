DROP TABLE `doiAssociation`;

CREATE TABLE `article` (
  `articleId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `doi` VARCHAR(150) NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`articleId`));

CREATE TABLE `articleIngestion` (
  `ingestionId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `articleId` BIGINT(20) NOT NULL,
  `ingestionNumber` INT NOT NULL,
  `title` TEXT NOT NULL,
  `publicationDate` DATE NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ingestionId`),
  CONSTRAINT `fk_articleIngestion_1`
    FOREIGN KEY (`articleId`)
    REFERENCES `article` (`articleId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `articleItem` (
  `itemId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `ingestionId` BIGINT(20) NOT NULL,
  `doi` VARCHAR(150) NOT NULL,
  `articleItemType` VARCHAR(128) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`itemId`),
  CONSTRAINT `fk_articleItem_1`
    FOREIGN KEY (`ingestionId`)
    REFERENCES `articleIngestion` (`ingestionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `articleFile` (
  `fileId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `ingestionId` BIGINT(20) NOT NULL,
  `itemId` BIGINT(20) NULL,
  `fileType` VARCHAR(128) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci',
  `bucketName` VARCHAR(255) NOT NULL,
  `crepoKey` VARCHAR(255) NOT NULL,
  `crepoUuid` VARCHAR(36) NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fileId`),
  CONSTRAINT `fk_articleFile_1`
    FOREIGN KEY (`ingestionId`)
    REFERENCES `articleIngestion` (`ingestionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_articleFile_2`
    FOREIGN KEY (`itemId`)
    REFERENCES `articleItem` (`itemId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  UNIQUE KEY `crepoUuid_UNIQUE` (`crepoUuid`));
  
CREATE TABLE `articleRevision` (
  `revisionId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `ingestionId` BIGINT(20) NOT NULL,
  `revisionNumber` INT NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`revisionId`),
  CONSTRAINT `fk_articleRevision_1`
    FOREIGN KEY (`ingestionId`)
    REFERENCES `articleIngestion` (`ingestionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `journal` (
  `journalId` bigint(20) NOT NULL AUTO_INCREMENT,
  `currentIssueId` bigint(20) DEFAULT NULL,
  `journalKey` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `eIssn` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `imageArticleId` bigint(20) DEFAULT NULL,
  `title` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`journalId`),
  KEY `journal_ibfk_1_idx` (`currentIssueId`),
  CONSTRAINT `fk_journal_1` FOREIGN KEY (`currentIssueId`) REFERENCES `issue` (`issueId`),
  CONSTRAINT `fk_journal_2` FOREIGN KEY (`imageArticleId`) REFERENCES `article` (`articleId`));

CREATE TABLE `articleJournalJoinTable` (
  `ingestionId` BIGINT(20) NOT NULL,
  `journalId` BIGINT(20) NOT NULL,
  INDEX `fk_articleJournalJoinTable_1_idx` (`ingestionId` ASC),
  INDEX `fk_articleJournalJoinTable_2_idx` (`journalID` ASC),
  CONSTRAINT `fk_articleJournalJoinTable_1`
    FOREIGN KEY (`ingestionId`)
    REFERENCES `articleIngestion` (`ingestionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_articleJournalJoinTable_2`
    FOREIGN KEY (`journalId`)
    REFERENCES `journal` (`journalID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);
