RENAME TABLE `journal` TO `oldJournal`;

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
  CONSTRAINT `fk_journal_2` FOREIGN KEY (`imageArticleId`) REFERENCES `article` (`articleId`)
);

-- temporary solution to be replaced by AccMan data migration script

INSERT INTO journal (eissn, journalKey, title) VALUES ("1545-7885", "PLoSBiology",        "PLOS Biology"                    );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1549-1676", "PLoSMedicine",       "PLOS Medicine"                   );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1553-7358", "PLoSCompBiol",       "PLOS Computational Biology"      );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1553-7374", "PLoSPathogens",      "PLOS Pathogens"                  );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1553-7404", "PLoSGenetics",       "PLOS Genetics"                   );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1555-5887", "PLoSClinicalTrials", "PLOS Clinical Trials"            );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1932-6203", "PLoSONE",            "PLOS ONE"                        );
INSERT INTO journal (eissn, journalKey, title) VALUES ("1935-2735", "PLoSNTD",            "PLOS Neglected Tropical Diseases");
INSERT INTO journal (eissn, journalKey, title) VALUES ("3333-3333", "PLoSCollections",    "PLOS Collections"                );