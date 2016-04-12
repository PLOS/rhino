DROP TABLE `articleRevision`;

DROP TABLE `doiAssociation`;

CREATE TABLE `ambra`.`scholarlyWork` (
  `scholarlyWorkId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `doi` VARCHAR(150) NOT NULL,
  `crepoKey` VARCHAR(255) NOT NULL,
  `crepoUuid` CHAR(36) NOT NULL,
  `scholarlyWorkType` VARCHAR(128) CHARACTER SET 'utf8' COLLATE 'utf8_unicode_ci' NOT NULL,
  PRIMARY KEY (`scholarlyWorkId`));

CREATE TABLE `ambra`.`scholarlyWorkRelation` (
  `targetWorkId` BIGINT(20) NOT NULL,
  `originWorkId` BIGINT(20) NOT NULL,
  `relationType` VARCHAR(45) NOT NULL,
  INDEX `fk_scholarlyWorkRelation_1_idx` (`targetWorkId` ASC),
  INDEX `fk_scholarlyWorkRelation_2_idx` (`originWorkId` ASC),
  CONSTRAINT `fk_scholarlyWorkRelation_1`
    FOREIGN KEY (`targetWorkId`)
    REFERENCES `ambra`.`scholarlyWork` (`scholarlyWorkId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_scholarlyWorkRelation_2`
    FOREIGN KEY (`originWorkId`)
    REFERENCES `ambra`.`scholarlyWork` (`scholarlyWorkId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);

CREATE TABLE `ambra`.`revision` (
  `revisionId` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `scholarlyWorkId` BIGINT(20) NOT NULL,
  `revisionNumber` INT NOT NULL,
  `publicationState` INT NOT NULL,
  PRIMARY KEY (`revisionId`),
  INDEX `fk_revision_1_idx` (`scholarlyWorkId` ASC),
  CONSTRAINT `fk_revision_1`
    FOREIGN KEY (`scholarlyWorkId`)
    REFERENCES `ambra`.`scholarlyWork` (`scholarlyWorkId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);
