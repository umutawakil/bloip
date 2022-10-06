-- MySQL dump 10.13  Distrib 5.7.21, for macos10.13 (x86_64)
--
-- Host: localhost    Database: bloip
-- ------------------------------------------------------
-- Server version	5.7.21

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
-- Table structure for table `comment`
--

DROP TABLE IF EXISTS `comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `comment` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `discussion_id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `track_number` int(11) DEFAULT '0',
  `creation_timestamp` datetime DEFAULT CURRENT_TIMESTAMP,
  `ip_address` varchar(300) COLLATE utf8_unicode_ci NOT NULL,
  `duration` int(11) NOT NULL,
  `file_name` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_comment_discussion_id_idx` (`discussion_id`),
  CONSTRAINT `fk_comment_discussion_id` FOREIGN KEY (`discussion_id`) REFERENCES `discussion` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=4099 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comment`
--

LOCK TABLES `comment` WRITE;
/*!40000 ALTER TABLE `comment` DISABLE KEYS */;
INSERT INTO `comment` VALUES (4092,2087,275,0,'2022-10-03 17:43:41','0:0:0:0:0:0:0:1',4,'275-d0b99085-ec77-4b46-a419-5e235fa1c9af.mp3'),(4093,2088,276,0,'2022-10-03 23:28:10','0:0:0:0:0:0:0:1',1,'276-838766fc-9b9b-4f06-aec3-69688fc2cb8e.mp3'),(4094,2089,276,0,'2022-10-04 15:01:10','0:0:0:0:0:0:0:1',1,'276-f8fe7bee-b39b-49c8-9537-e76c8b02540d.mp3'),(4095,2089,277,1,'2022-10-04 15:09:34','127.0.0.1',3,'277-563e5774-b2fa-4fde-bf4b-c8b1a9719fa9.mp3'),(4096,2089,277,2,'2022-10-04 15:10:42','127.0.0.1',2,'277-032ca8c7-4a2a-4514-812a-6ea58a25a468.mp3'),(4097,2089,277,3,'2022-10-04 15:11:21','127.0.0.1',3,'277-7f86eb11-b07e-4485-a93c-76a8267038d5.mp3'),(4098,2090,276,0,'2022-10-04 15:14:08','0:0:0:0:0:0:0:1',11,'276-4da0a50b-9a40-4c81-aa2d-19a37aeddbe2.mp3');
/*!40000 ALTER TABLE `comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `discussion`
--

DROP TABLE IF EXISTS `discussion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `discussion` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `creation_timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `number_of_replies` int(11) DEFAULT '0',
  `ip_address` varchar(45) COLLATE utf8_unicode_ci NOT NULL,
  `update_timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `topic_id` bigint(20) NOT NULL,
  `file_name` varchar(200) COLLATE utf8_unicode_ci DEFAULT NULL,
  `youtube_link` varchar(200) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_discussion_user_id_idx` (`user_id`),
  KEY `fk_discussion_topic_id_idx` (`topic_id`),
  KEY `idx_discussion_update_timestamp` (`update_timestamp`),
  CONSTRAINT `fk_discussion_topic_id` FOREIGN KEY (`topic_id`) REFERENCES `topic` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=2091 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `discussion`
--

LOCK TABLES `discussion` WRITE;
/*!40000 ALTER TABLE `discussion` DISABLE KEYS */;
INSERT INTO `discussion` VALUES (2087,'This is just a test to see how things look with long names.',275,'2022-10-03 21:43:42',0,'0:0:0:0:0:0:0:1','2022-10-05 21:06:17',3,'275-d0b99085-ec77-4b46-a419-5e235fa1c9af.mp3','https://www.youtube.com/watch?v=7h7oqlG_FxE'),(2088,'Another test for the view on mediumnames',276,'2022-10-04 03:28:10',0,'0:0:0:0:0:0:0:1','2022-10-05 21:06:17',3,'276-838766fc-9b9b-4f06-aec3-69688fc2cb8e.mp3','https://www.youtube.com/watch?v=7h7oqlG_FxE'),(2089,'How do things look if you use a really really long name?',276,'2022-10-04 19:01:10',3,'0:0:0:0:0:0:0:1','2022-10-05 21:06:17',3,'276-f8fe7bee-b39b-49c8-9537-e76c8b02540d.mp3',NULL),(2090,'Is Voldomort Zelensky just playing tricks on the US?',276,'2022-10-04 19:14:08',0,'0:0:0:0:0:0:0:1','2022-10-05 21:06:17',3,'276-4da0a50b-9a40-4c81-aa2d-19a37aeddbe2.mp3',NULL);
/*!40000 ALTER TABLE `discussion` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `discussion_subscription`
--

DROP TABLE IF EXISTS `discussion_subscription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `discussion_subscription` (
  `discussion_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`discussion_id`,`user_id`),
  KEY `fk_discussion_subscription_user_id_idx` (`user_id`),
  KEY `idx_discussion_subscription_did_user_id` (`discussion_id`,`user_id`),
  CONSTRAINT `fk_discussion_subscription_discussion_id` FOREIGN KEY (`discussion_id`) REFERENCES `discussion` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_discussion_subscription_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `discussion_subscription`
--

LOCK TABLES `discussion_subscription` WRITE;
/*!40000 ALTER TABLE `discussion_subscription` DISABLE KEYS */;
INSERT INTO `discussion_subscription` VALUES (2087,275),(2088,276),(2089,276),(2090,276),(2089,277);
/*!40000 ALTER TABLE `discussion_subscription` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inbox`
--

DROP TABLE IF EXISTS `inbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `inbox` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `discussion_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `creation_timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `unread` tinyint(1) DEFAULT '1',
  `title` varchar(300) COLLATE utf8_unicode_ci DEFAULT NULL,
  `track_number` int(11) DEFAULT '0',
  `last_update_timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `count` int(11) NOT NULL DEFAULT '0',
  `subscribed` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `fk_inbox_user_id_idx` (`user_id`),
  KEY `fk_inbox_discussion_id_idx` (`discussion_id`),
  CONSTRAINT `fk_inbox_discussion_id` FOREIGN KEY (`discussion_id`) REFERENCES `discussion` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION,
  CONSTRAINT `fk_inbox_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=283 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inbox`
--

LOCK TABLES `inbox` WRITE;
/*!40000 ALTER TABLE `inbox` DISABLE KEYS */;
INSERT INTO `inbox` VALUES (282,2089,276,'2022-10-04 19:09:34',0,'tryrtyrtyryr',1,'2022-10-04 19:09:34',2,1);
/*!40000 ALTER TABLE `inbox` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `topic`
--

DROP TABLE IF EXISTS `topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `topic` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8_unicode_ci NOT NULL,
  `description` varchar(300) COLLATE utf8_unicode_ci NOT NULL,
  `country` varchar(45) COLLATE utf8_unicode_ci DEFAULT 'US',
  `language` varchar(45) COLLATE utf8_unicode_ci DEFAULT 'en',
  `friendly_id` varchar(100) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=709 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `topic`
--

LOCK TABLES `topic` WRITE;
/*!40000 ALTER TABLE `topic` DISABLE KEYS */;
INSERT INTO `topic` VALUES (0,'Politics','What are your thoughts on a recent political issue?','US','en','politics'),(1,'Random','Start a conversation about anything','US','en','random'),(2,'Movies / TV','How do you feel about a show or movie you just watched?','US','en','hollywood'),(3,'Jokes','Tell a joke. Say something funny.','US','en','jokes'),(4,'Vocals','Sing a song or bust a rhyme!!!','US','en','rap'),(6,'Suggestion Box','Have any cool ideas for this site? Post them here or @BloipApp on twitter.','US','en','suggestion-box');
/*!40000 ALTER TABLE `topic` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `creation_timestamp` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=279 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (272,'2022-09-30 03:10:43'),(273,'2022-10-01 02:51:02'),(274,'2022-10-02 02:44:49'),(275,'2022-10-03 15:55:07'),(276,'2022-10-03 23:27:36'),(277,'2022-10-04 15:02:05'),(278,'2022-10-04 17:07:34');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_cookie`
--

DROP TABLE IF EXISTS `user_cookie`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_cookie` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(300) COLLATE utf8_unicode_ci NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `ip_address` varchar(300) COLLATE utf8_unicode_ci NOT NULL,
  `creation_timestamp` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code_UNIQUE` (`code`),
  KEY `user_cookie_user_id_idx` (`user_id`),
  CONSTRAINT `fk_user_cookie_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=278 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_cookie`
--

LOCK TABLES `user_cookie` WRITE;
/*!40000 ALTER TABLE `user_cookie` DISABLE KEYS */;
INSERT INTO `user_cookie` VALUES (88,'ac6299d1-97a0-4ff8-a276-4c517e2438da',272,'0:0:0:0:0:0:0:1','2022-10-01 02:51:01'),(129,'5d9efa45-3118-4681-907f-7b05da74e68f',273,'0:0:0:0:0:0:0:1','2022-10-02 02:44:47'),(170,'3e48cf7a-7045-4165-bbfe-8078e0724a1c',274,'0:0:0:0:0:0:0:1','2022-10-03 15:55:05'),(185,'e6568c46-5e37-41cf-89ef-ba0b8652178a',275,'0:0:0:0:0:0:0:1','2022-10-03 23:27:35'),(229,'c5a2457b-90bd-42e6-a291-4d01b1f6b756',277,'127.0.0.1','2022-10-04 15:02:06'),(234,'955acc6d-76c5-43f3-a226-9f3eae1e041e',276,'0:0:0:0:0:0:0:1','2022-10-04 17:07:32'),(277,'64bedef3-97c9-4ae2-a470-081f63b6aebf',278,'0:0:0:0:0:0:0:1','2022-10-05 21:58:27');
/*!40000 ALTER TABLE `user_cookie` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `web_push_notification_stat`
--

DROP TABLE IF EXISTS `web_push_notification_stat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `web_push_notification_stat` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `daily_count` int(11) DEFAULT '0',
  `last_update_timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `total_sent` int(11) DEFAULT '0',
  `total_clicked` int(11) DEFAULT '0',
  `total_received` int(11) DEFAULT '0',
  `needs_notification` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_wpns_needs_notification` (`needs_notification`),
  KEY `fk_web_push_notification_stat_user_id_idx` (`user_id`),
  CONSTRAINT `fk_web_push_notification_stat_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=147 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `web_push_notification_stat`
--

LOCK TABLES `web_push_notification_stat` WRITE;
/*!40000 ALTER TABLE `web_push_notification_stat` DISABLE KEYS */;
/*!40000 ALTER TABLE `web_push_notification_stat` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `web_push_subscription`
--

DROP TABLE IF EXISTS `web_push_subscription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `web_push_subscription` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `private_key` varchar(300) COLLATE utf8_unicode_ci NOT NULL,
  `auth` varchar(300) COLLATE utf8_unicode_ci NOT NULL,
  `endpoint` varchar(300) COLLATE utf8_unicode_ci NOT NULL,
  `expiration_time` varchar(30) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_web_push_subscription_user_id_idx` (`user_id`),
  CONSTRAINT `fk_web_push_subscription_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=157 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `web_push_subscription`
--

LOCK TABLES `web_push_subscription` WRITE;
/*!40000 ALTER TABLE `web_push_subscription` DISABLE KEYS */;
/*!40000 ALTER TABLE `web_push_subscription` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-10-05 22:34:16
