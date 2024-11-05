-- MySQL dump 10.13  Distrib 8.0.38, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: calculator
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
-- Table structure for table `data`
--

DROP TABLE IF EXISTS `data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `data` (
  `userId` varchar(255) NOT NULL,
  `date` double NOT NULL,
  `calculator` int NOT NULL,
  `data` longtext,
  `insertationId` int NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`insertationId`),
  UNIQUE KEY `insertationId_UNIQUE` (`insertationId`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `data`
--

LOCK TABLES `data` WRITE;
/*!40000 ALTER TABLE `data` DISABLE KEYS */;
INSERT INTO `data` VALUES ('K1VolLEyUWMm4vapNzSNgOT9Zn33',1726952400000,0,'76057;3725;3954;2726',5),('sQv9fp7KE5Og279Uw2M5csNHoWj1',1730581200000,0,'3100;4629;9244;8385',6),('sQv9fp7KE5Og279Uw2M5csNHoWj1',1730408400000,0,'9138;5310;6010;1982',7),('sQv9fp7KE5Og279Uw2M5csNHoWj1',1730322000000,0,'2500;123;4532;1245',8),('sQv9fp7KE5Og279Uw2M5csNHoWj1',1730235600000,0,'6286;2462;476;7605',9),('sQv9fp7KE5Og279Uw2M5csNHoWj1',1730581200000,1,'7460;6743;3199;1419',10),('sQv9fp7KE5Og279Uw2M5csNHoWj1',1730408400000,1,'1256;1471;8112;4374',11),('sQv9fp7KE5Og279Uw2M5csNHoWj1',1730322000000,1,'3029;1184;2430;7291',12),('sQv9fp7KE5Og279Uw2M5csNHoWj1',1730235600000,1,'1436;7425;4865;4047',13),('sQv9fp7KE5Og279Uw2M5csNHoWj1',1730754000000,0,'15955;22288;9668;11379',14),('K1VolLEyUWMm4vapNzSNgOT9Zn33',1730754000000,0,'10000;3725;3954;2726',21);
/*!40000 ALTER TABLE `data` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-11-05 22:23:50