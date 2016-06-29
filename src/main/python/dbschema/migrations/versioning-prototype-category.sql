CREATE TABLE `articleCategoryAssignment` (
  `articleId` bigint(20) NOT NULL,
  `categoryId` bigint(20) NOT NULL,
  `weight` int(11) NOT NULL,
  PRIMARY KEY (`articleId`,`categoryId`),
  KEY `articleId` (`articleId`),
  KEY `categoryId` (`categoryId`),
  CONSTRAINT `fk_articleCategoryAssignment_1`
  FOREIGN KEY (`articleId`)
  REFERENCES `article` (`articleId`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION,
  CONSTRAINT `fk_articleCategoryAssignment_2`
  FOREIGN KEY (`categoryId`)
  REFERENCES `category` (`categoryId`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION
);