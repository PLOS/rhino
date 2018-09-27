-- MySQL dump 10.13  Distrib 5.6.39-83.1, for debian-linux-gnu (x86_64)
--
-- Host: db-ambra.soma.plos.org    Database: ambra
-- ------------------------------------------------------
-- Server version	5.6.39-83.1-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `annotation`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `annotation` (
  `annotationID` bigint(20) NOT NULL AUTO_INCREMENT,
  `annotationURI` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `articleID` bigint(20) DEFAULT NULL,
  `parentID` bigint(20) DEFAULT NULL,
  `userProfileID` bigint(20) NOT NULL,
  `type` varchar(16) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `title` text CHARACTER SET utf8 COLLATE utf8_bin,
  `body` text CHARACTER SET utf8 COLLATE utf8_bin,
  `competingInterestBody` text CHARACTER SET utf8 COLLATE utf8_bin,
  `highlightedText` text CHARACTER SET utf8 COLLATE utf8_bin,
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  `isRemoved` bit(1) DEFAULT b'0',
  PRIMARY KEY (`annotationID`),
  UNIQUE KEY `annotationURI` (`annotationURI`),
  KEY `articleID` (`articleID`),
  KEY `parentID` (`parentID`),
  KEY `userProfileID` (`userProfileID`),
  CONSTRAINT `annotation_ibfk_1` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`),
  CONSTRAINT `annotation_ibfk_3` FOREIGN KEY (`parentID`) REFERENCES `annotation` (`annotationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `annotationFlag`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `annotationFlag` (
  `annotationFlagID` bigint(20) NOT NULL AUTO_INCREMENT,
  `annotationID` bigint(20) NOT NULL,
  `userProfileID` bigint(20) NOT NULL,
  `reason` varchar(25) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `comment` text CHARACTER SET utf8 COLLATE utf8_bin,
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  PRIMARY KEY (`annotationFlagID`),
  KEY `annotationID` (`annotationID`),
  KEY `userProfileID` (`userProfileID`),
  CONSTRAINT `annotationFlag_ibfk_1` FOREIGN KEY (`annotationID`) REFERENCES `annotation` (`annotationID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `article`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `article` (
  `articleId` bigint(20) NOT NULL AUTO_INCREMENT,
  `doi` varchar(150) COLLATE utf8_bin NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`articleId`),
  UNIQUE KEY `doi` (`doi`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleAsset`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleAsset` (
  `articleAssetID` bigint(20) NOT NULL AUTO_INCREMENT,
  `articleID` bigint(20) DEFAULT NULL,
  `doi` varchar(150) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `contextElement` varchar(30) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `contentType` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `extension` varchar(20) DEFAULT NULL,
  `title` varchar(500) DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `size` bigint(20) DEFAULT NULL,
  `sortOrder` int(11) DEFAULT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime DEFAULT NULL,
  PRIMARY KEY (`articleAssetID`),
  UNIQUE KEY `doi_2` (`doi`,`extension`),
  KEY `articleID` (`articleID`),
  KEY `doi` (`doi`),
  CONSTRAINT `__articleAsset_ibfk_1` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleCategoryAssignment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleCategoryAssignment` (
  `articleId` bigint(20) NOT NULL,
  `categoryId` bigint(20) NOT NULL,
  `weight` int(11) NOT NULL,
  PRIMARY KEY (`articleId`,`categoryId`),
  KEY `articleId` (`articleId`),
  KEY `categoryId` (`categoryId`),
  CONSTRAINT `fk_articleCategoryAssignment_1` FOREIGN KEY (`articleId`) REFERENCES `article` (`articleId`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_articleCategoryAssignment_2` FOREIGN KEY (`categoryId`) REFERENCES `category` (`categoryId`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleCategoryAssignmentFlag`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleCategoryAssignmentFlag` (
  `flagId` bigint(20) NOT NULL AUTO_INCREMENT,
  `articleId` bigint(20) NOT NULL,
  `categoryId` bigint(20) NOT NULL,
  `userProfileId` bigint(20) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`flagId`),
  KEY `articleId` (`articleId`),
  KEY `categoryId` (`categoryId`),
  KEY `fk_articleCategoryAssignmentFlag_1` (`articleId`,`categoryId`),
  CONSTRAINT `fk_articleCategoryAssignmentFlag_1` FOREIGN KEY (`articleId`, `categoryId`) REFERENCES `articleCategoryAssignment` (`articleId`, `categoryId`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleCategoryFlagged`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleCategoryFlagged` (
  `articleID` bigint(20) NOT NULL,
  `categoryID` bigint(20) NOT NULL,
  `userProfileID` bigint(20) DEFAULT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  UNIQUE KEY `articleID` (`articleID`,`categoryID`,`userProfileID`),
  KEY `categoryID` (`categoryID`),
  KEY `userProfileID` (`userProfileID`),
  CONSTRAINT `articleCategoryFlagged_ibfk_1` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`),
  CONSTRAINT `articleCategoryFlagged_ibfk_2` FOREIGN KEY (`categoryID`) REFERENCES `category` (`categoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleCategoryJoinTable`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleCategoryJoinTable` (
  `articleID` bigint(20) NOT NULL,
  `categoryID` bigint(20) NOT NULL,
  `weight` int(11) NOT NULL,
  PRIMARY KEY (`articleID`,`categoryID`),
  KEY `articleID` (`articleID`),
  KEY `categoryID` (`categoryID`),
  CONSTRAINT `articleCategoryJoinTable_ibfk_1` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`),
  CONSTRAINT `articleCategoryJoinTable_ibfk_2` FOREIGN KEY (`categoryID`) REFERENCES `category` (`categoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleCollaborativeAuthors`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleCollaborativeAuthors` (
  `articleID` bigint(20) NOT NULL,
  `sortOrder` int(11) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`articleID`,`sortOrder`),
  KEY `articleID` (`articleID`),
  CONSTRAINT `articleCollaborativeAuthors_ibfk_1` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleFile`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleFile` (
  `fileId` bigint(20) NOT NULL AUTO_INCREMENT,
  `ingestionId` bigint(20) NOT NULL,
  `itemId` bigint(20) DEFAULT NULL,
  `fileType` varchar(128) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `bucketName` varchar(255) COLLATE utf8_bin NOT NULL,
  `crepoKey` varchar(255) COLLATE utf8_bin NOT NULL,
  `crepoUuid` varchar(36) COLLATE utf8_bin NOT NULL,
  `fileSize` bigint(20) NOT NULL,
  `ingestedFileName` varchar(255) COLLATE utf8_bin NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fileId`),
  UNIQUE KEY `ingest_ingestedFileName` (`ingestionId`,`ingestedFileName`),
  UNIQUE KEY `crepoUuid_UNIQUE` (`crepoUuid`),
  UNIQUE KEY `ingest_item_filetype` (`ingestionId`,`itemId`,`fileType`),
  KEY `fk_articleFile_2` (`itemId`),
  KEY `bucketName_index` (`bucketName`),
  CONSTRAINT `fk_articleFile_1` FOREIGN KEY (`ingestionId`) REFERENCES `articleIngestion` (`ingestionId`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_articleFile_2` FOREIGN KEY (`itemId`) REFERENCES `articleItem` (`itemId`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleIngestion`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleIngestion` (
  `ingestionId` bigint(20) NOT NULL AUTO_INCREMENT,
  `articleId` bigint(20) NOT NULL,
  `journalId` bigint(20) NOT NULL,
  `ingestionNumber` int(11) NOT NULL,
  `title` text COLLATE utf8_bin NOT NULL,
  `publicationDate` date NOT NULL,
  `revisionDate` date DEFAULT NULL,
  `publicationStage` varchar(100) COLLATE utf8_bin DEFAULT NULL,
  `articleType` varchar(100) COLLATE utf8_bin DEFAULT NULL,
  `strikingImageItemId` bigint(20) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `preprintDoi` varchar(250) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ingestionId`),
  UNIQUE KEY `article_ingestnum` (`articleId`,`ingestionNumber`),
  KEY `fk_articleIngestion_2` (`journalId`),
  KEY `fk_articleIngestion_3` (`strikingImageItemId`),
  CONSTRAINT `fk_articleIngestion_1` FOREIGN KEY (`articleId`) REFERENCES `article` (`articleId`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_articleIngestion_2` FOREIGN KEY (`journalId`) REFERENCES `journal` (`journalId`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  CONSTRAINT `fk_articleIngestion_3` FOREIGN KEY (`strikingImageItemId`) REFERENCES `articleItem` (`itemId`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleItem`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleItem` (
  `itemId` bigint(20) NOT NULL AUTO_INCREMENT,
  `ingestionId` bigint(20) NOT NULL,
  `doi` varchar(150) COLLATE utf8_bin NOT NULL,
  `articleItemType` varchar(128) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`itemId`),
  KEY `doi` (`doi`),
  KEY `fk_articleItem_1` (`ingestionId`),
  CONSTRAINT `fk_articleItem_1` FOREIGN KEY (`ingestionId`) REFERENCES `articleIngestion` (`ingestionId`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleList`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleList` (
  `articleListId` bigint(20) NOT NULL AUTO_INCREMENT,
  `listKey` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `displayName` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `journalId` bigint(20) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `listType` varchar(255) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`articleListId`),
  UNIQUE KEY `listIdentity` (`journalId`,`listType`,`listKey`),
  KEY `journalId` (`journalId`),
  CONSTRAINT `fk_articleList_1` FOREIGN KEY (`journalId`) REFERENCES `journal` (`journalId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleListJoinTable`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleListJoinTable` (
  `articleListId` bigint(20) NOT NULL,
  `sortOrder` int(11) NOT NULL,
  `articleId` bigint(20) NOT NULL,
  PRIMARY KEY (`articleListId`,`sortOrder`),
  KEY `articleId` (`articleId`),
  CONSTRAINT `fk_articleListJoinTable_1` FOREIGN KEY (`articleListId`) REFERENCES `articleList` (`articleListId`),
  CONSTRAINT `fk_articleListJoinTable_2` FOREIGN KEY (`articleId`) REFERENCES `article` (`articleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articlePerson`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articlePerson` (
  `articlePersonID` bigint(20) NOT NULL AUTO_INCREMENT,
  `articleID` bigint(20) DEFAULT NULL,
  `sortOrder` int(11) DEFAULT NULL,
  `type` varchar(15) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `fullName` varchar(200) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `givenNames` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `surnames` varchar(200) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `suffix` varchar(15) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime DEFAULT NULL,
  PRIMARY KEY (`articlePersonID`),
  KEY `articleID` (`articleID`),
  CONSTRAINT `articlePerson_ibfk_1` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articlePublishedJournals`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articlePublishedJournals` (
  `articleID` bigint(20) NOT NULL,
  `journalID` bigint(20) NOT NULL,
  PRIMARY KEY (`articleID`,`journalID`),
  KEY `journalID` (`journalID`),
  CONSTRAINT `articlePublishedJournals_ibfk_1` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`),
  CONSTRAINT `articlePublishedJournals_ibfk_2` FOREIGN KEY (`journalID`) REFERENCES `oldJournal` (`journalID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleRelationship`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleRelationship` (
  `articleRelationshipId` bigint(20) NOT NULL AUTO_INCREMENT,
  `sourceArticleId` bigint(20) NOT NULL,
  `targetArticleId` bigint(20) NOT NULL,
  `type` varchar(50) COLLATE utf8_bin NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`articleRelationshipId`),
  KEY `sourceArticleId` (`sourceArticleId`),
  KEY `targetArticleId` (`targetArticleId`),
  CONSTRAINT `fk_articleRelationship_1` FOREIGN KEY (`sourceArticleId`) REFERENCES `article` (`articleId`),
  CONSTRAINT `fk_articleRelationship_2` FOREIGN KEY (`targetArticleId`) REFERENCES `article` (`articleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleRevision`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleRevision` (
  `revisionId` bigint(20) NOT NULL AUTO_INCREMENT,
  `ingestionId` bigint(20) NOT NULL,
  `revisionNumber` int(11) NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`revisionId`),
  KEY `fk_articleRevision_1` (`ingestionId`),
  CONSTRAINT `fk_articleRevision_1` FOREIGN KEY (`ingestionId`) REFERENCES `articleIngestion` (`ingestionId`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `articleType`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `articleType` (
  `articleID` bigint(20) NOT NULL,
  `type` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  KEY `articleID` (`articleID`),
  CONSTRAINT `articleType_ibfk_1` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `category`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `category` (
  `categoryId` bigint(20) NOT NULL AUTO_INCREMENT,
  `path` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`categoryId`),
  UNIQUE KEY `path` (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `categoryFeaturedArticle`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `categoryFeaturedArticle` (
  `categoryFeaturedArticleID` bigint(20) NOT NULL AUTO_INCREMENT,
  `journalID` bigint(20) NOT NULL,
  `articleID` bigint(20) NOT NULL,
  `category` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime DEFAULT NULL,
  PRIMARY KEY (`categoryFeaturedArticleID`),
  UNIQUE KEY `journalID` (`journalID`,`category`),
  KEY `articleID` (`articleID`),
  CONSTRAINT `categoryFeaturedArticle_ibfk_1` FOREIGN KEY (`journalID`) REFERENCES `oldJournal` (`journalID`),
  CONSTRAINT `categoryFeaturedArticle_ibfk_2` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `citedArticle`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `citedArticle` (
  `citedArticleID` bigint(20) NOT NULL AUTO_INCREMENT,
  `articleID` bigint(20) DEFAULT NULL,
  `keyColumn` varchar(10) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `year` int(11) DEFAULT NULL,
  `displayYear` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `month` varchar(15) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `day` varchar(20) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `volumeNumber` int(11) DEFAULT NULL,
  `volume` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `issue` varchar(60) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `title` text CHARACTER SET utf8 COLLATE utf8_bin,
  `publisherLocation` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `publisherName` text CHARACTER SET utf8 COLLATE utf8_bin,
  `pages` varchar(150) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `eLocationID` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `journal` varchar(250) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `note` text CHARACTER SET utf8 COLLATE utf8_bin,
  `url` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `doi` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `pmid` varchar(9) DEFAULT NULL,
  `citationType` varchar(60) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `summary` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `sortOrder` int(11) DEFAULT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime DEFAULT NULL,
  PRIMARY KEY (`citedArticleID`),
  KEY `articleID` (`articleID`),
  CONSTRAINT `citedArticle_ibfk_1` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `citedArticleCollaborativeAuthors`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `citedArticleCollaborativeAuthors` (
  `citedArticleID` bigint(20) NOT NULL,
  `sortOrder` int(11) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`citedArticleID`,`sortOrder`),
  KEY `citedArticleID` (`citedArticleID`),
  CONSTRAINT `citedArticleCollaborativeAuthors_ibfk_1` FOREIGN KEY (`citedArticleID`) REFERENCES `citedArticle` (`citedArticleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `citedPerson`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `citedPerson` (
  `citedPersonID` bigint(20) NOT NULL AUTO_INCREMENT,
  `citedArticleID` bigint(20) DEFAULT NULL,
  `type` varchar(15) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `fullName` varchar(200) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `givenNames` varchar(150) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `surnames` varchar(200) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `suffix` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `sortOrder` int(11) DEFAULT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime DEFAULT NULL,
  PRIMARY KEY (`citedPersonID`),
  KEY `citedArticleID` (`citedArticleID`),
  CONSTRAINT `citedPerson_ibfk_1` FOREIGN KEY (`citedArticleID`) REFERENCES `citedArticle` (`citedArticleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `comment`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `comment` (
  `commentId` bigint(20) NOT NULL AUTO_INCREMENT,
  `commentURI` varchar(255) COLLATE utf8_bin NOT NULL,
  `articleId` bigint(20) NOT NULL,
  `parentId` bigint(20) DEFAULT NULL,
  `userProfileId` bigint(20) DEFAULT NULL,
  `title` text COLLATE utf8_bin,
  `body` text COLLATE utf8_bin,
  `competingInterestBody` text COLLATE utf8_bin,
  `highlightedText` text COLLATE utf8_bin,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isRemoved` bit(1) DEFAULT b'0',
  `authorEmailAddress` varchar(250) COLLATE utf8_bin DEFAULT NULL,
  `authorName` varchar(250) COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`commentId`),
  UNIQUE KEY `commentURI` (`commentURI`),
  KEY `fk_comment_1` (`articleId`),
  KEY `fk_comment_2` (`parentId`),
  KEY `userProfileID` (`userProfileId`),
  CONSTRAINT `fk_comment_1` FOREIGN KEY (`articleId`) REFERENCES `article` (`articleId`),
  CONSTRAINT `fk_comment_2` FOREIGN KEY (`parentId`) REFERENCES `comment` (`commentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `commentFlag`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `commentFlag` (
  `commentFlagId` bigint(20) NOT NULL AUTO_INCREMENT,
  `commentId` bigint(20) NOT NULL,
  `userProfileId` bigint(20) DEFAULT NULL,
  `reason` varchar(25) COLLATE utf8_bin NOT NULL,
  `comment` text COLLATE utf8_bin,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`commentFlagId`),
  KEY `commentId` (`commentId`),
  KEY `userProfileId` (`userProfileId`),
  CONSTRAINT `fk_commentFlag_1` FOREIGN KEY (`commentId`) REFERENCES `comment` (`commentId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `issue`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `issue` (
  `issueId` bigint(20) NOT NULL AUTO_INCREMENT,
  `doi` varchar(150) COLLATE utf8_bin NOT NULL,
  `volumeId` bigint(20) NOT NULL,
  `volumeSortOrder` int(11) NOT NULL,
  `displayName` varchar(255) COLLATE utf8_bin NOT NULL,
  `imageArticleId` bigint(20) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`issueId`),
  UNIQUE KEY `doi` (`doi`),
  KEY `volumeId` (`volumeId`),
  KEY `fk_issue_2` (`imageArticleId`),
  CONSTRAINT `fk_issue_1` FOREIGN KEY (`volumeId`) REFERENCES `volume` (`volumeId`),
  CONSTRAINT `fk_issue_2` FOREIGN KEY (`imageArticleId`) REFERENCES `article` (`articleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `issueArticleList`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `issueArticleList` (
  `issueId` bigint(20) NOT NULL,
  `sortOrder` int(11) NOT NULL,
  `articleId` bigint(20) NOT NULL,
  PRIMARY KEY (`issueId`,`articleId`),
  KEY `fk_issueArticleList_2` (`articleId`),
  CONSTRAINT `fk_issueArticleList_1` FOREIGN KEY (`issueId`) REFERENCES `issue` (`issueId`),
  CONSTRAINT `fk_issueArticleList_2` FOREIGN KEY (`articleId`) REFERENCES `article` (`articleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `jira_ambr_291`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `jira_ambr_291` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(15) COLLATE utf8_bin NOT NULL,
  `description` varchar(50) COLLATE utf8_bin DEFAULT NULL,
  `created` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `name_idx` (`name`),
  KEY `oldest_idx` (`updated`,`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `journal`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `journal` (
  `journalId` bigint(20) NOT NULL AUTO_INCREMENT,
  `currentIssueId` bigint(20) DEFAULT NULL,
  `journalKey` varchar(255) COLLATE utf8_bin NOT NULL,
  `eIssn` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `title` varchar(500) COLLATE utf8_bin NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`journalId`),
  KEY `journal_ibfk_1_idx` (`currentIssueId`),
  CONSTRAINT `fk_journal_1` FOREIGN KEY (`currentIssueId`) REFERENCES `issue` (`issueId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `migration_status_log`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `migration_status_log` (
  `articleId` bigint(20) DEFAULT NULL,
  `migration_date` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `migration_status` int(11) DEFAULT NULL,
  `error_message` varchar(255) COLLATE utf8_bin DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `objects`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `objects` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `bucketId` int(11) DEFAULT NULL,
  `objkey` varchar(255) COLLATE utf8_bin NOT NULL,
  `checksum` varchar(255) COLLATE utf8_bin NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `downloadName` varchar(1000) COLLATE utf8_bin DEFAULT NULL,
  `contentType` varchar(200) COLLATE utf8_bin DEFAULT NULL,
  `size` int(11) NOT NULL,
  `tag` varchar(200) COLLATE utf8_bin DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `versionNumber` int(11) NOT NULL,
  `creationDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `uuid` char(36) COLLATE utf8_bin NOT NULL,
  `userMetadata` longtext COLLATE utf8_bin,
  PRIMARY KEY (`id`),
  UNIQUE KEY `keyVersion` (`bucketId`,`objkey`,`versionNumber`),
  UNIQUE KEY `ObjkeyUniversalID` (`bucketId`,`objkey`,`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oldArticle`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oldArticle` (
  `articleID` bigint(20) NOT NULL AUTO_INCREMENT,
  `doi` varchar(150) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `title` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `eIssn` varchar(15) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `state` int(11) NOT NULL,
  `archiveName` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `rights` text CHARACTER SET utf8 COLLATE utf8_bin,
  `language` varchar(5) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `format` varchar(10) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `date` datetime NOT NULL,
  `volume` varchar(5) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `issue` varchar(5) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `journal` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `publisherLocation` varchar(25) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `publisherName` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `pages` varchar(15) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `eLocationID` varchar(150) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `url` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `strkImgURI` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime DEFAULT NULL,
  PRIMARY KEY (`articleID`),
  KEY `doi` (`doi`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oldArticleList`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oldArticleList` (
  `articleListID` bigint(20) NOT NULL AUTO_INCREMENT,
  `listKey` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `displayName` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `journalID` bigint(20) DEFAULT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  `listType` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`articleListID`),
  UNIQUE KEY `listIdentity` (`journalID`,`listType`,`listKey`),
  CONSTRAINT `oldArticleList_ibfk_1` FOREIGN KEY (`journalID`) REFERENCES `oldJournal` (`journalID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oldArticleListJoinTable`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oldArticleListJoinTable` (
  `articleListID` bigint(20) NOT NULL,
  `sortOrder` int(11) NOT NULL,
  `articleID` bigint(20) NOT NULL,
  PRIMARY KEY (`articleListID`,`sortOrder`),
  KEY `articleID` (`articleID`),
  CONSTRAINT `oldArticleListJoinTable_ibfk_1` FOREIGN KEY (`articleListID`) REFERENCES `oldArticleList` (`articleListID`),
  CONSTRAINT `oldArticleListJoinTable_ibfk_2` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oldArticleRelationship`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oldArticleRelationship` (
  `articleRelationshipID` bigint(20) NOT NULL AUTO_INCREMENT,
  `parentArticleID` bigint(20) NOT NULL,
  `otherArticleDoi` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `otherArticleID` bigint(20) DEFAULT NULL,
  `type` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `sortOrder` int(11) NOT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime DEFAULT NULL,
  PRIMARY KEY (`articleRelationshipID`),
  KEY `parentArticleID` (`parentArticleID`),
  KEY `otherArticleID` (`otherArticleID`),
  CONSTRAINT `oldArticleRelationship_ibfk_1` FOREIGN KEY (`parentArticleID`) REFERENCES `oldArticle` (`articleID`),
  CONSTRAINT `oldArticleRelationship_ibfk_2` FOREIGN KEY (`otherArticleID`) REFERENCES `oldArticle` (`articleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oldIssue`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oldIssue` (
  `issueID` bigint(20) NOT NULL AUTO_INCREMENT,
  `issueUri` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `volumeID` bigint(20) DEFAULT NULL,
  `volumeSortOrder` int(11) DEFAULT NULL,
  `displayName` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `respectOrder` bit(1) DEFAULT NULL,
  `imageUri` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `title` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `description` text CHARACTER SET utf8 COLLATE utf8_bin,
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  PRIMARY KEY (`issueID`),
  UNIQUE KEY `issueUri` (`issueUri`),
  KEY `volumeID` (`volumeID`),
  CONSTRAINT `oldIssue_ibfk_1` FOREIGN KEY (`volumeID`) REFERENCES `oldVolume` (`volumeID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oldIssueArticleList`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oldIssueArticleList` (
  `issueID` bigint(20) NOT NULL DEFAULT '0',
  `sortOrder` int(11) NOT NULL DEFAULT '0',
  `doi` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  PRIMARY KEY (`issueID`,`sortOrder`),
  CONSTRAINT `oldIssueArticleList_ibfk_1` FOREIGN KEY (`issueID`) REFERENCES `oldIssue` (`issueID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oldJournal`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oldJournal` (
  `journalID` bigint(20) NOT NULL AUTO_INCREMENT,
  `currentIssueID` bigint(20) DEFAULT NULL,
  `journalKey` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `eIssn` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `imageUri` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `title` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `description` text CHARACTER SET utf8 COLLATE utf8_bin,
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  PRIMARY KEY (`journalID`),
  KEY `currentIssueID` (`currentIssueID`),
  CONSTRAINT `oldJournal_ibfk_1` FOREIGN KEY (`currentIssueID`) REFERENCES `oldIssue` (`issueID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oldSyndication`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oldSyndication` (
  `syndicationID` bigint(20) NOT NULL AUTO_INCREMENT,
  `doi` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `target` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `status` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `submissionCount` int(11) DEFAULT NULL,
  `errorMessage` longtext CHARACTER SET utf8 COLLATE utf8_bin,
  `created` datetime NOT NULL,
  `lastSubmitTimestamp` datetime DEFAULT NULL,
  `lastModified` datetime DEFAULT NULL,
  PRIMARY KEY (`syndicationID`),
  UNIQUE KEY `doi` (`doi`,`target`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oldVolume`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `oldVolume` (
  `volumeID` bigint(20) NOT NULL AUTO_INCREMENT,
  `volumeUri` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `journalID` bigint(20) DEFAULT NULL,
  `journalSortOrder` int(11) DEFAULT NULL,
  `displayName` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `imageUri` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `title` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `description` text CHARACTER SET utf8 COLLATE utf8_bin,
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  PRIMARY KEY (`volumeID`),
  UNIQUE KEY `volumeUri` (`volumeUri`),
  KEY `journalID` (`journalID`),
  CONSTRAINT `oldVolume_ibfk_1` FOREIGN KEY (`journalID`) REFERENCES `oldJournal` (`journalID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pingback`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pingback` (
  `pingbackID` bigint(20) NOT NULL AUTO_INCREMENT,
  `articleID` bigint(20) NOT NULL,
  `url` varchar(255) COLLATE utf8_bin NOT NULL,
  `title` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime DEFAULT NULL,
  PRIMARY KEY (`pingbackID`),
  UNIQUE KEY `articleID` (`articleID`,`url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `savedSearch`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `savedSearch` (
  `savedSearchID` bigint(20) NOT NULL AUTO_INCREMENT,
  `userProfileID` bigint(20) NOT NULL,
  `savedSearchQueryID` bigint(20) NOT NULL,
  `searchName` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `searchType` varchar(16) NOT NULL,
  `lastWeeklySearchTime` datetime NOT NULL,
  `lastMonthlySearchTime` datetime NOT NULL,
  `monthly` bit(1) DEFAULT b'0',
  `weekly` bit(1) DEFAULT b'0',
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  PRIMARY KEY (`savedSearchID`),
  UNIQUE KEY `userProfileID` (`userProfileID`,`searchName`),
  KEY `savedSearchQueryID` (`savedSearchQueryID`),
  CONSTRAINT `savedSearch_ibfk_1` FOREIGN KEY (`userProfileID`) REFERENCES `userProfile` (`userProfileID`),
  CONSTRAINT `savedSearch_ibfk_2` FOREIGN KEY (`savedSearchQueryID`) REFERENCES `savedSearchQuery` (`savedSearchQueryID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `savedSearchQuery`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `savedSearchQuery` (
  `savedSearchQueryID` bigint(20) NOT NULL AUTO_INCREMENT,
  `searchParams` text CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `hash` varchar(50) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `created` datetime NOT NULL,
  `lastmodified` datetime NOT NULL,
  PRIMARY KEY (`savedSearchQueryID`),
  UNIQUE KEY `hash_2` (`hash`),
  KEY `hash` (`hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `syndication`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `syndication` (
  `syndicationId` bigint(20) NOT NULL AUTO_INCREMENT,
  `revisionId` bigint(20) NOT NULL,
  `targetQueue` varchar(50) COLLATE utf8_bin NOT NULL,
  `status` varchar(50) COLLATE utf8_bin NOT NULL,
  `submissionCount` int(11) DEFAULT NULL,
  `errorMessage` longtext COLLATE utf8_bin,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastSubmitTimestamp` timestamp NULL DEFAULT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`syndicationId`),
  UNIQUE KEY `revisionId` (`revisionId`,`targetQueue`),
  CONSTRAINT `fk_syndication_1` FOREIGN KEY (`revisionId`) REFERENCES `articleRevision` (`revisionId`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trackback`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trackback` (
  `trackbackID` bigint(20) NOT NULL AUTO_INCREMENT,
  `articleID` bigint(20) NOT NULL,
  `url` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `title` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `blogname` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `excerpt` text CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  PRIMARY KEY (`trackbackID`),
  KEY `articleID` (`articleID`),
  CONSTRAINT `trackback_ibfk_1` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userArticleView`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userArticleView` (
  `userArticleViewID` bigint(20) NOT NULL AUTO_INCREMENT,
  `userProfileID` bigint(20) NOT NULL,
  `articleID` bigint(20) NOT NULL,
  `created` datetime NOT NULL,
  `type` varchar(20) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`userArticleViewID`),
  KEY `userProfileID` (`userProfileID`),
  KEY `articleID` (`articleID`),
  CONSTRAINT `userArticleView_ibfk_1` FOREIGN KEY (`userProfileID`) REFERENCES `userProfile` (`userProfileID`),
  CONSTRAINT `userArticleView_ibfk_2` FOREIGN KEY (`articleID`) REFERENCES `oldArticle` (`articleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userLogin`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userLogin` (
  `userLoginID` bigint(20) NOT NULL AUTO_INCREMENT,
  `userProfileID` bigint(20) NOT NULL,
  `sessionID` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `IP` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `userAgent` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `created` datetime NOT NULL,
  PRIMARY KEY (`userLoginID`),
  KEY `userProfileID` (`userProfileID`),
  CONSTRAINT `userLogin_ibfk_1` FOREIGN KEY (`userProfileID`) REFERENCES `userProfile` (`userProfileID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userOrcid`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userOrcid` (
  `userProfileID` bigint(20) NOT NULL,
  `orcid` varchar(25) COLLATE utf8_bin NOT NULL,
  `accessToken` varchar(50) COLLATE utf8_bin NOT NULL,
  `refreshToken` varchar(50) COLLATE utf8_bin NOT NULL,
  `tokenScope` varchar(100) COLLATE utf8_bin NOT NULL,
  `tokenExpires` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  `created` datetime NOT NULL,
  PRIMARY KEY (`userProfileID`),
  UNIQUE KEY `orcid` (`orcid`),
  CONSTRAINT `userOrcid_ibfk_1` FOREIGN KEY (`userProfileID`) REFERENCES `userProfile` (`userProfileID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userProfile`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userProfile` (
  `userProfileID` bigint(20) NOT NULL AUTO_INCREMENT,
  `userProfileURI` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `authId` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `realName` varchar(500) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `givenNames` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `surName` varchar(65) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `gender` varchar(15) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `homePage` varchar(512) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `weblog` varchar(512) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `publications` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `displayName` varchar(255) DEFAULT NULL,
  `suffix` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `positionType` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `organizationName` varchar(512) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `organizationType` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `organizationVisibility` tinyint(1) NOT NULL DEFAULT '0',
  `postalAddress` text CHARACTER SET utf8 COLLATE utf8_bin,
  `city` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `country` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `biography` text CHARACTER SET utf8 COLLATE utf8_bin,
  `interests` text CHARACTER SET utf8 COLLATE utf8_bin,
  `researchAreas` text CHARACTER SET utf8 COLLATE utf8_bin,
  `alertsJournals` text CHARACTER SET utf8 COLLATE utf8_bin,
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `passwordReset` bit(1) NOT NULL DEFAULT b'0',
  `verificationToken` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `verified` bit(1) NOT NULL DEFAULT b'1',
  `active` bit(1) NOT NULL DEFAULT b'1',
  PRIMARY KEY (`userProfileID`),
  UNIQUE KEY `userProfileURI` (`userProfileURI`),
  UNIQUE KEY `authId` (`authId`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `displayName` (`displayName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userProfileMetaData`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userProfileMetaData` (
  `userProfileMetaDataID` bigint(20) NOT NULL AUTO_INCREMENT,
  `userProfileID` bigint(20) NOT NULL,
  `metaKey` varchar(50) COLLATE utf8_bin NOT NULL,
  `metaValue` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `lastModified` datetime NOT NULL,
  `created` datetime NOT NULL,
  PRIMARY KEY (`userProfileMetaDataID`),
  UNIQUE KEY `userProfileID` (`userProfileID`,`metaKey`),
  KEY `metaKey` (`metaKey`),
  CONSTRAINT `userProfileMetaData_ibfk_1` FOREIGN KEY (`userProfileID`) REFERENCES `userProfile` (`userProfileID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userProfileRoleJoinTable`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userProfileRoleJoinTable` (
  `userRoleID` bigint(20) NOT NULL,
  `userProfileID` bigint(20) NOT NULL,
  PRIMARY KEY (`userRoleID`,`userProfileID`),
  KEY `userProfileID` (`userProfileID`),
  CONSTRAINT `userProfileRoleJoinTable_ibfk_1` FOREIGN KEY (`userRoleID`) REFERENCES `userRole` (`userRoleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userRole`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userRole` (
  `userRoleID` bigint(20) NOT NULL AUTO_INCREMENT,
  `roleName` varchar(15) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime NOT NULL,
  PRIMARY KEY (`userRoleID`),
  UNIQUE KEY `roleName` (`roleName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userRolePermission`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userRolePermission` (
  `userRoleID` bigint(20) NOT NULL,
  `permission` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`userRoleID`,`permission`),
  CONSTRAINT `userRolePermission_ibfk_1` FOREIGN KEY (`userRoleID`) REFERENCES `userRole` (`userRoleID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userSearch`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userSearch` (
  `userSearchID` bigint(20) NOT NULL AUTO_INCREMENT,
  `userProfileID` bigint(20) NOT NULL,
  `searchTerms` text CHARACTER SET utf8 COLLATE utf8_bin,
  `searchString` text CHARACTER SET utf8 COLLATE utf8_bin,
  `created` datetime NOT NULL,
  PRIMARY KEY (`userSearchID`),
  KEY `userProfileID` (`userProfileID`),
  CONSTRAINT `userSearch_ibfk_1` FOREIGN KEY (`userProfileID`) REFERENCES `userProfile` (`userProfileID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `uuid_lut`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `uuid_lut` (
  `objkey` varchar(255) COLLATE utf8_bin NOT NULL,
  `size` int(11) NOT NULL,
  `versionNumber` int(11) NOT NULL,
  `uuid` char(36) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`objkey`),
  UNIQUE KEY `objkey_versionNumber` (`objkey`,`versionNumber`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `version`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `version` (
  `versionID` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(25) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `version` int(11) NOT NULL,
  `updateInProcess` bit(1) NOT NULL,
  `created` datetime NOT NULL,
  `lastModified` datetime DEFAULT NULL,
  PRIMARY KEY (`versionID`)
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `volume`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `volume` (
  `volumeId` bigint(20) NOT NULL AUTO_INCREMENT,
  `doi` varchar(150) COLLATE utf8_bin NOT NULL,
  `journalId` bigint(20) NOT NULL,
  `journalSortOrder` int(11) NOT NULL,
  `displayName` varchar(255) COLLATE utf8_bin NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`volumeId`),
  UNIQUE KEY `doi` (`doi`),
  KEY `journalID` (`journalId`),
  CONSTRAINT `fk_volume_1` FOREIGN KEY (`journalId`) REFERENCES `journal` (`journalId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2018-09-27 14:49:26
