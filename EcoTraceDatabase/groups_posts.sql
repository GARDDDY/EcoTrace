-- MySQL dump 10.13  Distrib 8.0.38, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: groups
-- ------------------------------------------------------
-- Server version	8.0.39

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `posts`
--

DROP TABLE IF EXISTS `posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `posts` (
  `groupId` varchar(255) NOT NULL,
  `postId` int NOT NULL AUTO_INCREMENT,
  `postTime` varchar(255) NOT NULL,
  `postCreatorId` varchar(255) NOT NULL,
  `postContentText` longtext,
  `postContentImage` varchar(255) DEFAULT NULL,
  UNIQUE KEY `postId_UNIQUE` (`postId`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `posts`
--

LOCK TABLES `posts` WRITE;
/*!40000 ALTER TABLE `posts` DISABLE KEYS */;
INSERT INTO `posts` VALUES ('group_1',1,'1726783001','sQv9fp7KE5Og279Uw2M5csNHoWj1','Тестовая запись!',NULL),('group_1',2,'1726783002','sQv9fp7KE5Og279Uw2M5csNHoWj1','Еще одна',NULL),('group_1',3,'1726783003','sQv9fp7KE5Og279Uw2M5csNHoWj1','И еще, 1',NULL),('group_1',4,'1726783004','sQv9fp7KE5Og279Uw2M5csNHoWj1','И еще, 2',NULL),('group_1',5,'1727379506','sQv9fp7KE5Og279Uw2M5csNHoWj1','Новая!',NULL),('group_1',6,'1727379706','sQv9fp7KE5Og279Uw2M5csNHoWj1','Новейшая!',NULL),('group_1',7,'1727899454','K1VolLEyUWMm4vapNzSNgOT9Zn33','gggrgrgrghrtht','1727899454495'),('group_1',8,'1727901386','K1VolLEyUWMm4vapNzSNgOT9Zn33',NULL,'1727901385990');
/*!40000 ALTER TABLE `posts` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-11-05 22:23:48