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
-- Table structure for table `userstatics`
--

DROP TABLE IF EXISTS `userstatics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `userstatics` (
  `userId` varchar(255) NOT NULL,
  `q00` varchar(255) DEFAULT '0;0',
  `q01` varchar(255) DEFAULT '0;0',
  `q02` varchar(255) DEFAULT '0;0',
  `q03` varchar(255) DEFAULT '0;0',
  `q10` varchar(255) DEFAULT '0,0',
  `q11` varchar(255) DEFAULT '0,0',
  `q12` varchar(255) DEFAULT '0,0',
  `q13` varchar(255) DEFAULT '0,0',
  `q14` varchar(255) DEFAULT '0,0',
  `q20` varchar(255) DEFAULT '0,0',
  `q21` varchar(255) DEFAULT '0,0',
  `q22` varchar(255) DEFAULT '0,0',
  `q23` varchar(255) DEFAULT '0,0',
  `q24` varchar(255) DEFAULT '0,0',
  `q30` varchar(255) DEFAULT '0,0',
  `q31` varchar(255) DEFAULT '0,0',
  `q32` varchar(255) DEFAULT '0,0',
  `q33` varchar(255) DEFAULT '0,0',
  `q34` varchar(255) DEFAULT '0,0',
  `q40` varchar(255) DEFAULT '0,0',
  `q41` varchar(255) DEFAULT '0,0',
  `q42` varchar(255) DEFAULT '0,0',
  `q43` varchar(255) DEFAULT '0,0',
  `q44` varchar(255) DEFAULT '0,0',
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `userstatics`
--

LOCK TABLES `userstatics` WRITE;
/*!40000 ALTER TABLE `userstatics` DISABLE KEYS */;
INSERT INTO `userstatics` VALUES ('K1VolLEyUWMm4vapNzSNgOT9Zn33','43028;2','3725;2','3954;2','2726;2','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0'),('sQv9fp7KE5Og279Uw2M5csNHoWj1','1457;15','338;15','347;15','185;15','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0'),('USz4eAc','0;0','0;0','0;0','0;0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0'),('USz5kgt','0;0','0;0','0;0','0;0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0','0,0');
/*!40000 ALTER TABLE `userstatics` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-11-09 23:11:37
