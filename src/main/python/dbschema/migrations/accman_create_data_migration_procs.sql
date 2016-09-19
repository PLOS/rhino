# assumptions: the objkey_uuid extract from content repo has been added to the ambra db for use as a UUID/filesize lookup
#
# results of migration sent to migration_status_log
#
#
# Use this for initial migration attempt and modify select statement to choose specific articles as needed thereafter
# CALL migrate_articles();

####################################################################################################

DROP FUNCTION IF EXISTS `get_doi_name`;
DELIMITER $$
CREATE DEFINER=`root`@`localhost` FUNCTION `get_doi_name`(full_doi VARCHAR(150)) RETURNS varchar(150) CHARSET latin1
DETERMINISTIC
  BEGIN
    RETURN REPLACE(full_doi, 'info:doi/', '');
  END$$
DELIMITER ;

####################################################################################################

DROP PROCEDURE IF EXISTS `correct_article_asset_table()`;
DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `correct_article_asset_table`()
  BEGIN
    # see DPRO-2938 for justification of each statement

    # rows in articleAsset table where doi = artilce doi and extension is not XML or PDF
    DELETE FROM articleAsset WHERE articleAssetID IN (4325338, 4325330, 4325360, 4325348, 4325312, 4325320, 4325354,
      4325356, 4325346, 4325318, 4325358, 4325336, 4325322, 4768867, 6521805, 10816407, 11648965, 12074323, 17002131, 17016278);

    # malformed extension for inline TIF asset (not present in repo db so UUID lookup fails)
    DELETE FROM articleAsset WHERE articleAssetID IN (3261684, 3261686, 4004838, 3506626, 3276844, 3996370, 3617756,
      3387424, 3436982, 4107892, 3313340, 3676070, 3145172, 3269304, 3354794, 3730104, 3284248, 3671736, 3276846, 4011052,
      3238278, 2798598, 3554196, 4071214, 3430956, 3246144, 3238280, 3994200, 3261688, 3996372, 3489904, 2824584, 3994202,
      3107822, 3246146, 3253938, 3284250, 4006920, 4218776, 3246148, 3253940, 1207348, 3761232, 3738028, 3931960, 2444060,
      4090248, 1496856, 4019248, 4099226, 3538722, 2869282, 3276848, 2652614, 3734084, 1496858, 3693104, 3368024, 3598796,
      3549106, 3717992, 3327438, 2798600, 3253942, 3564266, 3608318, 3276852, 3730108, 3284252, 2769946, 3033834, 3269314,
      3974020, 2996694, 3400038, 3135944, 3772444, 3517478, 3097958, 3915706, 3693094, 4088712, 4090244, 3135946, 3654196,
      3824832, 3145054, 3495522, 3645260, 904138, 3713874, 3117298, 3327382, 1496428, 3033598, 3738022, 1993436, 3603534,
      3087912, 3424848, 262234, 3393766, 3291602, 3269302);

    # individual cases (mostly duplicate or extraneous assets not reachable in final article page or present in the repo)
    UPDATE articleAsset SET doi = 'info:doi/10.1371/journal.pbio.0020012.s003' WHERE articleAssetID = 1207360;
    UPDATE articleAsset SET doi = 'info:doi/10.1371/journal.pbio.0020012.s004' WHERE articleAssetID = 1496904;

    DELETE FROM articleAsset WHERE articleAssetID IN (2795796, 2822204, 2845876);

    UPDATE articleAsset SET contextElement = 'inline-formula' WHERE articleAssetID IN (2800526, 2811392);

    UPDATE articleAsset SET contextElement = 'disp-formula' WHERE articleAssetID IN (2906826, 18586085);

    DELETE FROM articleAsset WHERE articleAssetID = 4135480;

    DELETE FROM articleAsset WHERE articleAssetID = 2867706;

    DELETE FROM articleAsset WHERE articleAssetID = 2938856;

    DELETE FROM articleAsset WHERE articleAssetID IN (2847384, 2868804, 2888378, 2906612, 2923808, 2939944);

    DELETE FROM articleAsset WHERE articleAssetID = 6954501;

    DELETE FROM articleAsset WHERE articleAssetID = 8404193;

    DELETE FROM articleAsset WHERE articleAssetID = 8636225;

    DELETE FROM articleAsset WHERE articleAssetID = 9120333;

    DELETE FROM articleAsset WHERE articleAssetID = 8636225;

    DELETE FROM articleAsset WHERE articleAssetID = 9134183;

    DELETE FROM articleAsset WHERE articleAssetID = 9756233;

    DELETE FROM articleAsset WHERE articleAssetID = 9756959;

    DELETE FROM articleAsset WHERE articleAssetID = 9815499;

    DELETE FROM articleAsset WHERE articleAssetID = 9961945;

    DELETE FROM articleAsset WHERE articleAssetID = 11491203;

    DELETE FROM articleAsset WHERE articleAssetID IN (14789201, 14789193, 14789195, 14789197, 14789199);

    DELETE FROM articleAsset WHERE articleAssetID = 15588622;

    DELETE FROM articleAsset WHERE articleAssetID = 8975739;

  END $$
DELIMITER ;

####################################################################################################

DROP PROCEDURE IF EXISTS `create_uuid_lut`;
DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `create_uuid_lut`()
  BEGIN

    IF NOT EXISTS (SELECT * FROM information_schema.tables
                   WHERE table_schema = (SELECT DATABASE()) AND table_name = 'objects') THEN
      SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Please restore the ''objects'' table from the crepo db into the ambra db before proceeding';
    END IF;

    DROP TABLE IF EXISTS uuid_lut;
    CREATE TABLE uuid_lut (
      objkey varchar(255) COLLATE utf8_bin NOT NULL,
      size int(11) NOT NULL,
      versionNumber int(11) NOT NULL,
      uuid char(36) COLLATE utf8_bin NOT NULL,
      PRIMARY KEY (`objkey`));

    INSERT INTO uuid_lut (objkey, versionNumber)
      SELECT objkey, MAX(versionNumber)
      FROM objects WHERE bucketId = 1 AND `status` = 0
      GROUP BY objkey;
    UPDATE uuid_lut lut
      INNER JOIN objects o ON bucketId = 1 AND lut.objkey = o.objkey AND lut.versionNumber = o.versionNumber
      SET lut.uuid = o.uuid, lut.size = o.size;

END $$
DELIMITER ;

####################################################################################################

DROP PROCEDURE IF EXISTS `migrate_article`;
DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `migrate_article`(IN article_id BIGINT)
  BEGIN

    DECLARE pub_date DATE DEFAULT NULL;
    DECLARE article_title TEXT DEFAULT NULL;
    DECLARE title_XML_prefix TEXT;
    DECLARE title_XML_postfix TEXT;
    DECLARE article_doi VARCHAR(150);
    DECLARE article_doi_name VARCHAR(150) DEFAULT '';
    DECLARE ingestion_id BIGINT DEFAULT 0;
    DECLARE item_id BIGINT DEFAULT 0;
    DECLARE striking_image_item_id BIGINT;
    DECLARE striking_image_doi VARCHAR(150);
    DECLARE item_type VARCHAR(128);
    DECLARE file_type VARCHAR(128);
    DECLARE file_size BIGINT;
    DECLARE asset_doi VARCHAR(150);
    DECLARE asset_doi_name VARCHAR(150);
    DECLARE prev_asset_doi VARCHAR(150) DEFAULT '';
    DECLARE asset_extension VARCHAR(10);
    DECLARE context_element VARCHAR(30);
    DECLARE article_type VARCHAR(100);
    DECLARE journal_id BIGINT;
    DECLARE crepo_key VARCHAR(255);
    DECLARE crepo_uuid CHAR(36);
    DECLARE err_msg VARCHAR(1024);
    DECLARE done INT DEFAULT FALSE;
    DECLARE call_status INT DEFAULT -1;

    DECLARE old_assets_cursor CURSOR FOR
      SELECT doi, extension, contextElement
      FROM articleAsset
      WHERE articleID = article_id
            AND extension NOT IN ('ORIG','ZIP','ZIP_PART') AND doi NOT LIKE '%.t___-M' # bogus recs and safe to ignore
      ORDER BY doi;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    IF EXISTS (SELECT * FROM article WHERE articleID = article_id) THEN
      SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'Article already exists in target table';
    ELSE
      SELECT doi, title, `date`, strkImgURI
      INTO article_doi, article_title, pub_date, striking_image_doi
      FROM oldArticle
      WHERE articleID = article_id;

      IF article_doi <> '' THEN

        SET article_doi_name = get_doi_name(article_doi);
        INSERT INTO article (articleId, doi, created)
        VALUES (article_id, article_doi_name, NOW());

        # use capitalized article type and remove URI prefix and escaped spaces (i.e. "http://rdf.plos.org/RDF/articleType/Message%20from%20PLoS" becomes "Message from PLoS")
        SELECT REPLACE(REPLACE(MIN(type),'http://rdf.plos.org/RDF/articleType/',''),'%20',' ')
        INTO article_type
        FROM articleType
        WHERE articleID = article_id AND type IS NOT NULL;

        # fix the couple cases where the capitalized version is not present (only occurs in research article type)
        IF article_type = 'research-article' THEN
          SET article_type = 'Research Article';
        END IF;

        # find published journal (use MAX() to select clinical trials journal for cross-published records)
        SELECT MAX(journalID)
        INTO journal_id
        FROM articlePublishedJournals
        WHERE articleID = article_id;

        SET title_XML_prefix = '<article-title xmlns:mml="http://www.w3.org/1998/Math/MathML" xmlns:xlink="http://www.w3.org/1999/xlink">';
        SET title_XML_postfix = '</article-title>';
        INSERT INTO articleIngestion (articleId, ingestionNumber, journalId, title, publicationDate, articleType, created, lastModified)
        VALUES (article_id, 1, journal_id, CONCAT(title_XML_prefix, article_title, title_XML_postfix), pub_date, article_type, NOW(), NOW());

        SET ingestion_id = LAST_INSERT_ID();

        INSERT INTO articleRevision (ingestionId, revisionNumber, created)
        VALUES (ingestion_id, 1, NOW());

        OPEN old_assets_cursor;
        persist_assets_loop: LOOP

          SET done = FALSE ;
          FETCH old_assets_cursor INTO asset_doi, asset_extension, context_element;
          IF done THEN
            LEAVE persist_assets_loop;
          END IF;

          IF asset_doi <> prev_asset_doi THEN
            SET asset_doi_name = get_doi_name(asset_doi);
            SET item_type =
            CASE
            WHEN asset_doi LIKE '%.strk' THEN 'standaloneStrikingImage'
            WHEN asset_doi_name = article_doi_name THEN 'article'
            WHEN context_element = 'fig' THEN 'figure'
            WHEN context_element = 'table-wrap' THEN 'table'
            WHEN context_element = 'disp-formula' OR context_element = 'inline-formula' THEN 'graphic'
            WHEN context_element = 'supplementary-material' THEN 'supplementaryMaterial'
            WHEN asset_doi LIKE '%.g___' THEN 'figure'
            WHEN asset_doi LIKE '%.t___' THEN 'table'
            WHEN asset_doi LIKE '%.e___' OR asset_doi LIKE '%.m___' OR asset_doi LIKE '%logo' THEN 'graphic'
            WHEN asset_doi LIKE '%.s___' OR asset_doi LIKE '%.sd___' THEN 'supplementaryMaterial'
            ELSE 'unknown'
            END;

            IF item_type = 'unknown' THEN
              SET err_msg = CONCAT('Item type for ', get_doi_name(asset_doi), ' not found. Rolling back ', get_doi_name(article_doi));
              CALL migrate_article_rollback(article_id);
              SIGNAL SQLSTATE '45000'
              SET MESSAGE_TEXT = err_msg;
            END IF;

            INSERT INTO articleItem
            (ingestionId, doi, articleItemType, created)
            VALUES (ingestion_id, asset_doi_name, item_type, NOW());

            SET item_id = LAST_INSERT_ID();

            IF asset_doi = striking_image_doi THEN
              SET striking_image_item_id = item_id;
            END IF;
          END IF;

          SET crepo_key = CONCAT(get_doi_name(asset_doi), '.', asset_extension);
          SET crepo_uuid = '';
          SELECT uuid, size INTO crepo_uuid, file_size FROM uuid_lut WHERE objkey = crepo_key;
          IF crepo_uuid = '' THEN
            SET err_msg = CONCAT('Crepo key ', crepo_key, ' not found. Rolling back ', get_doi_name(article_doi));
            CALL migrate_article_rollback(article_id);
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = err_msg;
          END IF;

          SET file_type =
          CASE
          WHEN item_type = 'standaloneStrikingImage' THEN 'strikingImage'
          WHEN item_type = 'article' THEN
            CASE
            WHEN asset_extension = 'XML' THEN 'manuscript'
            WHEN asset_extension = 'PDF' THEN 'printable'
            ELSE 'Unknown article'
            END
          WHEN item_type = 'figure' THEN
            CASE
            WHEN asset_extension = 'PNG_S' THEN 'small'
            WHEN asset_extension = 'PNG_M' THEN 'medium'
            WHEN asset_extension = 'PNG_L' THEN 'large'
            WHEN asset_extension = 'PNG_I' THEN 'inline'
            WHEN asset_extension = 'TIFF' OR asset_extension = 'TIF' OR asset_extension = 'GIF' THEN 'original'
            ELSE 'Unknown figure'
            END
          WHEN item_type = 'table' THEN
            CASE
            WHEN asset_extension = 'PNG_S' THEN 'small'
            WHEN asset_extension = 'PNG_M' THEN 'medium'
            WHEN asset_extension = 'PNG_L' THEN 'large'
            WHEN asset_extension = 'PNG_I' THEN 'inline'
            WHEN asset_extension = 'TIFF' OR asset_extension = 'TIF' OR asset_extension = 'GIF' THEN 'original'
            ELSE 'Unknown table'
            END
          WHEN item_type = 'graphic' THEN
            CASE WHEN asset_extension = 'TIF' OR asset_extension = 'GIF' THEN 'original'
            WHEN asset_extension = 'PNG' THEN 'thumbnail'
            ELSE 'Unknown graphic'
            END
          WHEN item_type = 'supplementaryMaterial' THEN 'supplementary'
          ELSE 'Unknown'
          END;

          IF file_type LIKE 'unknown%' THEN
            SET err_msg = CONCAT(file_type, ' file type for ', get_doi_name(asset_doi), '. Rolling back ', get_doi_name(article_doi));
            CALL migrate_article_rollback(article_id);
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = err_msg;
          END IF;

          INSERT INTO articleFile
          (ingestionId, itemId, bucketName, crepoKey, crepoUuid, created, fileType, fileSize)
          VALUES (ingestion_id, item_id, 'mogilefs-prod-repo', crepo_key, crepo_uuid, NOW(), file_type, file_size);

          SET prev_asset_doi = asset_doi;

        END LOOP persist_assets_loop;
        CLOSE old_assets_cursor;

        IF striking_image_item_id IS NOT NULL THEN
          UPDATE articleIngestion SET strikingImageItemId = striking_image_item_id WHERE ingestionId = ingestion_id;
        END IF;

      ELSE
        SET err_msg = CONCAT('Article with ID=', article_id, ' not found in source table');
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = err_msg;
      END IF;
    END IF;

    # log as success
    INSERT INTO migration_status_log (articleId, migration_date, migration_status)
    VALUES (article_id, NOW(), 0);
  END$$
DELIMITER ;

####################################################################################################

DROP PROCEDURE IF EXISTS `migrate_articles`;
DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `migrate_articles`()
  BEGIN

    DECLARE article_id BIGINT DEFAULT NULL;
    DECLARE err_num INT;
    DECLARE err_state VARCHAR(128);
    DECLARE err_msg VARCHAR(1024);
    DECLARE full_err_msg VARCHAR(1024);
    DECLARE done INT DEFAULT FALSE;
    DECLARE commit_skip_max INT DEFAULT 100;
    DECLARE commit_skip_num INT;

    ##### MODIFY THIS SQL STATEMENT TO SELECT OTHER THAN ALL RECORDS IF A SECONDARY MIGRATION RUN IS DESIRED #####
    DECLARE old_articles_cursor CURSOR FOR
      SELECT articleID FROM oldArticle WHERE articleID NOT IN (SELECT articleId FROM article)
      ORDER BY articleID;
    #  ORDER BY rand();
    #LIMIT 10;
    #WHERE doi LIKE '%pbio.1002509%';

    DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
    BEGIN
      GET DIAGNOSTICS CONDITION 1 err_state = RETURNED_SQLSTATE,
      err_num = MYSQL_ERRNO, err_msg = MESSAGE_TEXT;
      SET full_err_msg = CONCAT("ERROR ", err_num, " (", err_state, "): ", err_msg);
      INSERT INTO migration_status_log (articleId, migration_date, migration_status, error_message)
      VALUES (article_id, NOW(), 1, full_err_msg);
      CALL migrate_article_rollback(article_id);
    END;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    CALL correct_article_asset_table();

    IF NOT EXISTS (SELECT * FROM information_schema.tables
      WHERE table_schema = (SELECT DATABASE()) AND table_name = 'uuid_lut') THEN
      CALL create_uuid_lut();
    END IF;

    CREATE TABLE IF NOT EXISTS migration_status_log (
      articleId BIGINT(20) NOT NULL,
      migration_date DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
      migration_status INT(11) DEFAULT NULL,
      error_message VARCHAR(255) DEFAULT NULL);

    # replace the temporary records added by the migrate.py script
    IF EXISTS (SELECT journalID FROM oldJournal WHERE journalID NOT IN (SELECT journalId FROM journal)) THEN
      SET FOREIGN_KEY_CHECKS=0;
      DELETE FROM journal;
      INSERT INTO journal (journalId, journalKey, eIssn, title, created, lastModified)
        SELECT journalID, journalKey, eIssn, title, created, lastModified
        FROM oldJournal;
      SET FOREIGN_KEY_CHECKS=1;
    END IF;

    SET commit_skip_num = 0;
    SET autocommit = 0;
    START TRANSACTION;

    OPEN old_articles_cursor;
    article_loop: LOOP

      FETCH old_articles_cursor INTO article_id;
      IF done THEN
        LEAVE article_loop;
      END IF;

      CALL migrate_article(article_id);

      SET commit_skip_num = commit_skip_num + 1;
      IF commit_skip_num > commit_skip_max THEN
        COMMIT;
        SET commit_skip_num = 0;
      END IF;

    END LOOP;
    CLOSE old_articles_cursor;

    COMMIT;
    SET autocommit = 1;
    
    # the insert statements below are designed to be able to run incrementally without causing
    # duplication errors in the case we need to run a secondary migration for a partial list of articles IDs

    INSERT INTO articleList
      SELECT * FROM oldArticleList
      WHERE articleListID NOT IN (SELECT articleListId FROM articleList);

    INSERT INTO articleListJoinTable
      SELECT aljt.* FROM oldArticleListJoinTable aljt INNER JOIN article a ON aljt.articleID = a.articleId
      WHERE aljt.articleID NOT IN (SELECT articleId FROM articleListJoinTable WHERE articleListID = aljt.articleListID);

    INSERT INTO volume (volumeId, doi, journalId, journalSortOrder, displayName, created, lastModified)
      SELECT volumeID, volumeUri, journalID, journalSortOrder, displayName, created, lastModified
      FROM oldVolume
      WHERE volumeID NOT IN (SELECT volumeId FROM volume);

    # fix data issues before migrating (see DPRO-2868). Once that ticket is done, this should not be necessary
    DELETE FROM oldIssueArticleList WHERE issueID = 2791;
    DELETE FROM oldIssue WHERE volumeID IS NULL; #issueID = 2791 and 2786, the latter of which has no records in oldIssueArticleList
    UPDATE oldIssue SET issueUri = 'info:doi/10.1371/issue.ppat.v12.i01' WHERE issueUri = 'info:doi/10.1371/issue.ppat.v12.i012'; # issueID=2793
    UPDATE oldIssue SET issueUri = 'info:doi/10.1371/issue.ppat.v12.i02' WHERE issueUri = 'info:doi/10.1371/issue.ppat.v12.i022'; # issueID=2798

    INSERT INTO issue (issueId, doi, volumeId, volumeSortOrder, displayName, created, lastModified)
      SELECT issueID, get_doi_name(issueUri), volumeID, volumeSortOrder, displayName, created, lastModified
      FROM oldIssue
      WHERE issueId NOT IN (SELECT issueId FROM issue);

    UPDATE issue SET imageArticleId = (SELECT articleID FROM article INNER JOIN oldIssue ON get_doi_name(imageUri) = doi WHERE issueID = issue.issueId);

    UPDATE journal SET currentIssueId = (SELECT currentIssueID FROM oldJournal WHERE journalID = journal.journalId);

    INSERT INTO issueArticleList (issueId, sortOrder, articleId)
      SELECT issueID, sortOrder, a.articleId
      FROM oldIssueArticleList ial INNER JOIN oldArticle oa ON ial.doi = oa.doi INNER JOIN article a ON oa.articleID = a.articleId
      WHERE a.articleId NOT IN (SELECT articleId FROM issueArticleList WHERE issueId = ial.issueID);

    INSERT INTO articleCategoryAssignment
      SELECT acjt.* FROM articleCategoryJoinTable acjt INNER JOIN article a ON acjt.articleID = a.articleId
      WHERE acjt.articleID NOT IN (SELECT articleId FROM articleCategoryAssignment);

    SET FOREIGN_KEY_CHECKS=0; # necessary because of parent/child relationships
    INSERT INTO `comment` (commentId, commentURI, articleId, parentId, userProfileId, title, body, competingInterestBody, highlightedText, created, lastModified, isRemoved)
      SELECT annotationID, get_doi_name(annotationURI), ann.articleID, parentID, userProfileID, title, body, competingInterestBody, highlightedText, ann.created, ann.lastModified, isRemoved
      FROM annotation ann INNER JOIN article a ON ann.articleID = a.articleId
      WHERE ann.articleID NOT IN (SELECT articleId FROM comment);
    SET FOREIGN_KEY_CHECKS=1;

    INSERT INTO commentFlag
      SELECT af.* FROM annotationFlag af INNER JOIN `comment` c ON af.annotationID = c.commentId
      WHERE annotationFlagID NOT IN (SELECT commentFlagId FROM commentFlag);

    INSERT INTO syndication (syndicationId, targetQueue, status, submissionCount, errorMessage, created, lastSubmitTimestamp, lastModified, revisionId)
      SELECT syndicationID, target, `status`, submissionCount, errorMessage, os.created, lastSubmitTimestamp, os.lastModified,
        (SELECT revisionId FROM articleRevision ar INNER JOIN articleIngestion ai ON ar.ingestionId = ai.ingestionId WHERE ai.articleId = a.articleId)
      FROM oldSyndication os INNER JOIN oldArticle oa ON os.doi = oa.doi INNER JOIN article a ON oa.articleID = a.articleId
      WHERE os.syndicationID NOT IN (SELECT syndicationId FROM syndication);

    INSERT INTO articleRelationship (articleRelationshipId, sourceArticleId, targetArticleId, type, created, lastModified)
      SELECT articleRelationshipID, parentArticleID, otherArticleID, type, ar.created, ar.lastModified
      FROM oldArticleRelationship ar INNER JOIN article a1 ON ar.parentArticleID = a1.articleId INNER JOIN article a2 ON ar.otherArticleID = a2.articleId
      WHERE type NOT IN ('retraction', 'expressed-concern') AND articleRelationshipID NOT IN (SELECT articleRelationshipId FROM articleRelationship);

  END$$
DELIMITER ;

####################################################################################################

DROP PROCEDURE IF EXISTS `migrate_article_rollback`;
DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `migrate_article_rollback`(IN article_id BIGINT)
  BEGIN
    DECLARE ingestion_id BIGINT;

    DECLARE EXIT HANDLER FOR 1242
    SELECT 'More than one ingestion exists for the given article ID. Rollback aborted.';

    # Get ingestion id for migrated article. Throws error 1242 when more than one ingestion exists.
    # This should not be possible for migrated articles and aborts rollback.
    SET ingestion_id = (SELECT ingestionId FROM articleIngestion WHERE articleId = article_id);

    DELETE FROM commentFlag WHERE commentId in (SELECT commentId FROM `comment` WHERE articleId = article_id);
    SET FOREIGN_KEY_CHECKS=0; # necessary because of parent/child relationships
    DELETE FROM `comment` WHERE articleId = article_id;
    SET FOREIGN_KEY_CHECKS=1;
    DELETE FROM articleRelationship WHERE sourceArticleId = article_id OR targetArticleId = article_id;
    DELETE FROM articleCategoryAssignment WHERE articleId = article_id;
    DELETE FROM articleListJoinTable WHERE articleId = article_id;
    DELETE FROM issueArticleList WHERE articleId = article_id;
    DELETE FROM articleFile WHERE ingestionId = ingestion_id;
    UPDATE articleIngestion SET strikingImageItemId = NULL WHERE ingestionId = ingestion_id;
    DELETE FROM articleItem WHERE ingestionId = ingestion_id;
    DELETE FROM syndication WHERE revisionId IN (SELECT revisionId FROM articleRevision WHERE ingestionId = ingestion_id);
    DELETE FROM articleRevision WHERE ingestionId = ingestion_id;
    DELETE FROM articleIngestion WHERE ingestionId = ingestion_id;
    DELETE FROM article WHERE articleId = article_id;

  END$$
DELIMITER ;


####################################################################################################

DROP PROCEDURE IF EXISTS `migrate_article_rollback_all`;
DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `migrate_article_rollback_all`()
  BEGIN

    DECLARE article_id BIGINT DEFAULT NULL;
    DECLARE done INT DEFAULT FALSE;

    ##### MODIFY THIS SQL STATEMENT TO SELECT OTHER THAN ALL RECORDS IF DESIRED #####
    DECLARE old_articles_cursor CURSOR FOR
      SELECT articleId FROM article;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN old_articles_cursor;
    article_loop: LOOP

      FETCH old_articles_cursor INTO article_id;
      IF done THEN
        LEAVE article_loop;
      END IF;

      CALL migrate_article_rollback(article_id);

    END LOOP;
    CLOSE old_articles_cursor;

    IF NOT EXISTS (SELECT * FROM article) THEN
      DELETE FROM articleList;
      UPDATE journal SET currentIssueId = NULL;
      DELETE FROM issue;
      DELETE FROM volume;
      DELETE FROM journal;
    END IF;

  END$$
DELIMITER ;
