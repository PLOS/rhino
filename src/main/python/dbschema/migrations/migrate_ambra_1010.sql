ALTER TABLE `annotation`
ADD COLUMN `isRemoved` BIT(1) NULL DEFAULT 0 AFTER `lastModified`;