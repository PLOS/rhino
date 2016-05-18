DROP TABLE `articleRevision`;

DROP TABLE `doiAssociation`;

CREATE TABLE `ambra`.`scholarlyWork` (
  `scholarlyWorkId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `doi` VARCHAR(150) NOT NULL,
  `scholarlyWorkType` VARCHAR(128) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`scholarlyWorkId`));

CREATE TABLE `ambra`.`scholarlyWorkRelation` (
  `originWorkId` BIGINT(20) NOT NULL,
  `targetWorkId` BIGINT(20) NOT NULL,
  `relationType` VARCHAR(45) NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `ids_type_UNIQUE` (`originWorkId`, `targetWorkId`, `relationType`),
  INDEX `fk_scholarlyWorkRelation_1_idx` (`originWorkId` ASC),
  INDEX `fk_scholarlyWorkRelation_2_idx` (`targetWorkId` ASC),
  CONSTRAINT `fk_scholarlyWorkRelation_1`
    FOREIGN KEY (`originWorkId`)
    REFERENCES `ambra`.`scholarlyWork` (`scholarlyWorkId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_scholarlyWorkRelation_2`
    FOREIGN KEY (`targetWorkId`)
    REFERENCES `ambra`.`scholarlyWork` (`scholarlyWorkId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `ambra`.`revision` (
  `revisionId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `scholarlyWorkId` BIGINT(20) NOT NULL,
  `revisionNumber` INT NOT NULL,
  `publicationState` INT NOT NULL,
  `created` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`revisionId`),
  UNIQUE KEY `scholarlyWorkId_UNIQUE` (`scholarlyWorkId`),
  INDEX `fk_revision_1_idx` (`scholarlyWorkId` ASC),
  CONSTRAINT `fk_revision_1`
    FOREIGN KEY (`scholarlyWorkId`)
    REFERENCES `ambra`.`scholarlyWork` (`scholarlyWorkId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `ambra`.`scholarlyWorkFile` (
  `fileId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `scholarlyWorkId` BIGINT(20) NOT NULL,
  `crepoKey` VARCHAR(255) NOT NULL,
  `crepoUuid` VARCHAR(36) NOT NULL,
  `fileType` VARCHAR(128) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci',
  PRIMARY KEY (`fileId`),
  CONSTRAINT `fk_scholarlyWorkFile_1`
    FOREIGN KEY (`scholarlyWorkId`)
    REFERENCES `ambra`.`scholarlyWork` (`scholarlyWorkId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
    UNIQUE KEY `crepoUuid_UNIQUE` (`crepoUuid`));
