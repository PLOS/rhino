CREATE TABLE `ambra`.`scholarlyWorkJournalJoinTable` (
  `scholarlyWorkID` BIGINT(20) NOT NULL,
  `journalID` BIGINT(20) NOT NULL,
  INDEX `fk_scholarlyWorkJournalJoinTable_1_idx` (`scholarlyWorkID` ASC),
  INDEX `fk_scholarlyWorkJournalJoinTable_2_idx` (`journalID` ASC),
  CONSTRAINT `fk_scholarlyWorkJournalJoinTable_1`
    FOREIGN KEY (`scholarlyWorkID`)
    REFERENCES `ambra`.`scholarlyWork` (`scholarlyWorkId`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_scholarlyWorkJournalJoinTable_2`
    FOREIGN KEY (`journalID`)
    REFERENCES `ambra`.`journal` (`journalID`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION);
