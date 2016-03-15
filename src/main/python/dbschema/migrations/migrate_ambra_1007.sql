ALTER TABLE articleList

  -- Rename listKey to listKey
  CHANGE COLUMN listCode listKey varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,

  -- Add the listType column
  ADD COLUMN listType varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,

  -- Replace the global uniqueness constraint on listKeys with (journalID, listType, listKey)
  -- (i.e., listKeys will be unique only within their own journal and among the same type)
  DROP INDEX listCode,
  ADD UNIQUE KEY listIdentity (journalID, listType, listKey),

  -- Article lists are no longer ordered within each journal
  DROP COLUMN journalSortOrder;

-- Fill a constant value as the listType for all pre-existing articleLists, then add a non-null constraint.
-- (We do NOT want to declare "admin" as the default when adding the column. This would have accomplished the same
-- thing during the backfill, but we want to use "admin" only during the backfill, not as a default for future rows.)
UPDATE articleList SET listType="admin";
ALTER TABLE articleList
  MODIFY COLUMN listType varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL;


--
-- Replace raw DOIs with foreign keys of actual article rows. This is a multi-step process.
--

-- Step 1 - Add an empty column that will be filled with article keys
ALTER TABLE articleListJoinTable ADD COLUMN articleID bigint DEFAULT NULL;

-- Step 2 - Fill the articleID column by joining existing DOIs from the article table
-- TODO - Needs error-handling in case articles are not present?
-- We can check in advance whether this will fail by running the following query by hand...
--   SELECT articleListJoinTable.doi
--     FROM articleListJoinTable LEFT OUTER JOIN article
--     ON articleListJoinTable.doi = article.doi
--     WHERE article.doi IS NULL;
UPDATE articleListJoinTable INNER JOIN article
  ON article.doi = articleListJoinTable.doi
  SET articleListJoinTable.articleID = article.articleID;

-- Step 3 - Set up constraints on the now-filled articleID column
ALTER TABLE articleListJoinTable
  MODIFY COLUMN articleID bigint NOT NULL,
  ADD CONSTRAINT FOREIGN KEY (articleID) REFERENCES article (articleID);

-- Step 4 - Drop the DOI column
ALTER TABLE articleListJoinTable DROP COLUMN doi;
