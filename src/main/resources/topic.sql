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
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `topic`
--

LOCK TABLES `topic` WRITE;
/*!40000 ALTER TABLE `topic` DISABLE KEYS */;
INSERT INTO `topic` VALUES (1,'Random','Start a conversation about anything','US','en','random'),(2,'News','Your thoughts on anything in the news lately','US','en','news'),(3,'Politics','Whats going on in the world of politics?','US','en','politics'),(4,'Racism','What are your thoughts on anything related to racism? Is it getting worse or blown out of proportion?','US','en','racism'),(5,'Climate Change / Environment','How do you feel about recent climate change or related events? Are we doing enough to address it or too little? Is it a real threat or a scam? Nucler or solar? Wind or hydro? Tesla or a gas powered car?','US','en','climate'),(6,'Economics & Poverty','Isues or events related to economics or poverty - Is the economy on the right track? What should be done differently?','US','en','econ'),(7,'Donald Trump & MAGA','Your thoughts on Donald Trump and whatever is going on in the MAGA world.','US','en','maga'),(8,'Joe Biden & BBB','Your commentary on Joe Biden and whatever is going on in the world of Build Back Better','US','en','bbb'),(9,'Crime & Punishment','Lets hear your thoughts on a recent criminal court case, or anything related to a crime that occurred recently.','US','en','crime'),(10,'Police Brutality / Law Enforcement','What are your thoughts on police brutality or any contreversial incidents involving the police? Is it a resisting arrest problem or excessive force? Do we need more cops or less?','US','en','cops'),(11,'LGBTQT+','Discuss anything related to the LGBTQ+ community','US','en','lgbtq+'),(12,'Illegal Immigration','Is there an illegal immigration problem? If so, whats the solution? If not, what\'s all the fuss about?','US','en','migrants'),(13,'Abortion','Should it be banned, legalized, or limited? What are your thoughts on abortion?','US','en','abortion'),(14,'Sexism','Are women being discriminated against? If so, how? If not, why do you think that?','US','en','sexism'),(15,'World Affairs / Foreign Policy','Your thoughts on things happening outside the United States','US','en','world'),(16,'Addiction / Substance Abuse','Any thoughts you have on addiction or substance abuse. Perhaps you have a personal story to tell or suggestions for addicts or society on how to solve the associated problems.','US','en','drugs'),(17,'Guns','Do we need better gun laws, more enforcement of the existing laws, or something different? How do you feel about guns and gun related incidents in the news lately?','US','en','guns'),(18,'Religion / Faith','Tell us your views on religion. Are there any related events in the news you think have religious implications?','US','en','God'),(19,'Sing a song!','Lets hear your vocal skills!','US','en','sing'),(20,'Bust a ryhme!','Lets hear you rap...freestyle!','US','en','rap'),(21,'Jokes / Comedy','Say something funny. Tell a joke. Give us all some comic releif.','US','en','funny'),(22,'Suggestion Box / Feature Requests','What improvments or special features would you like to see? You can also send messages to @BloipApp on twitter.','US','en','suggestion-box');
/*!40000 ALTER TABLE `topic` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-09-15 14:31:10
