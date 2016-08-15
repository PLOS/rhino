DROP TABLE `doiAssociation`;

RENAME TABLE `article` TO `oldArticle`;

CREATE TABLE `ambra`.`article` (
  `articleId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `doi` VARCHAR(150) NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`articleId`));

CREATE TABLE `ambra`.`articleIngestion` (
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
    REFERENCES `ambra`.`article` (`articleId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `ambra`.`articleItem` (
  `itemId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `ingestionId` BIGINT(20) NOT NULL,
  `doi` VARCHAR(150) NOT NULL,
  `articleItemType` VARCHAR(128) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`itemId`),
  CONSTRAINT `fk_articleItem_1`
    FOREIGN KEY (`ingestionId`)
    REFERENCES `ambra`.`articleIngestion` (`ingestionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `ambra`.`articleFile` (
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
    REFERENCES `ambra`.`articleIngestion` (`ingestionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_articleFile_2`
    FOREIGN KEY (`itemId`)
    REFERENCES `ambra`.`articleItem` (`itemId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  UNIQUE KEY `crepoUuid_UNIQUE` (`crepoUuid`));
  
CREATE TABLE `ambra`.`articleRevision` (
  `revisionId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `ingestionId` BIGINT(20) NOT NULL,
  `revisionNumber` INT NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`revisionId`),
  CONSTRAINT `fk_articleRevision_1`
    FOREIGN KEY (`ingestionId`)
    REFERENCES `ambra`.`articleIngestion` (`ingestionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `ambra`.`articleJournalJoinTable` (
  `ingestionId` BIGINT(20) NOT NULL,
  `journalId` BIGINT(20) NOT NULL,
  INDEX `fk_articleJournalJoinTable_1_idx` (`ingestionId` ASC),
  INDEX `fk_articleJournalJoinTable_2_idx` (`journalID` ASC),
  CONSTRAINT `fk_articleJournalJoinTable_1`
    FOREIGN KEY (`ingestionId`)
    REFERENCES `ambra`.`articleIngestion` (`ingestionId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_articleJournalJoinTable_2`
    FOREIGN KEY (`journalId`)
    REFERENCES `ambra`.`journal` (`journalID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);
