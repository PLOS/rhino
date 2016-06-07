DROP TABLE `articleRevision`;

DROP TABLE `doiAssociation`;

CREATE TABLE `ambra`.`endeavor` (
  `endeavorId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `doi` VARCHAR(150) NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`endeavorId`));

CREATE TABLE `ambra`.`ingestionEvent` (
  `ingestionEventId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `endeavorId` BIGINT(20) NOT NULL,
  `revisionNumber` INT NULL,
  `publicationState` INT NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` TIMESTAMP NOT NULL,
  PRIMARY KEY (`ingestionEventId`),
  CONSTRAINT `fk_ingestionEvent_1`
    FOREIGN KEY (`endeavorId`)
    REFERENCES `ambra`.`endeavor` (`endeavorId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `ambra`.`scholarlyWork` (
  `scholarlyWorkId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `ingestionEventId` BIGINT(20) NOT NULL,
  `doi` VARCHAR(150) NOT NULL,
  `scholarlyWorkType` VARCHAR(128) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`scholarlyWorkId`),
  CONSTRAINT `fk_scholarlyWork_1`
    FOREIGN KEY (`ingestionEventId`)
    REFERENCES `ambra`.`ingestionEvent` (`ingestionEventId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `ambra`.`ingestedFile` (
  `fileId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `ingestionEventId` BIGINT(20) NOT NULL,
  `scholarlyWorkId` BIGINT(20) NULL,
  `fileType` VARCHAR(128) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci',
  `crepoKey` VARCHAR(255) NOT NULL,
  `crepoUuid` VARCHAR(36) NOT NULL,
  PRIMARY KEY (`fileId`),
  CONSTRAINT `fk_ingestedFile_1`
    FOREIGN KEY (`ingestionEventId`)
    REFERENCES `ambra`.`ingestionEvent` (`ingestionEventId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_ingestedFile_2`
    FOREIGN KEY (`scholarlyWorkId`)
    REFERENCES `ambra`.`scholarlyWork` (`scholarlyWorkId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  UNIQUE KEY `crepoUuid_UNIQUE` (`crepoUuid`));

CREATE TABLE `ambra`.`publishedInJournal` (
  `ingestionEventId` BIGINT(20) NOT NULL,
  `journalId` BIGINT(20) NOT NULL,
  INDEX `fk_publishedInJournal_1_idx` (`ingestionEventId` ASC),
  INDEX `fk_publishedInJournal_2_idx` (`journalID` ASC),
  CONSTRAINT `fk_publishedInJournal_1`
    FOREIGN KEY (`ingestionEventId`)
    REFERENCES `ambra`.`ingestionEvent` (`ingestionEventId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_publishedInJournal_2`
    FOREIGN KEY (`journalId`)
    REFERENCES `ambra`.`journal` (`journalID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);
