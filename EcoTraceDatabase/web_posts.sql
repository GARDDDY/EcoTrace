-- MySQL dump 10.13  Distrib 8.0.38, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: web
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
  `source` varchar(255) DEFAULT NULL,
  `postLink` varchar(255) DEFAULT NULL,
  `postImage` varchar(255) DEFAULT NULL,
  `postTitle` varchar(255) DEFAULT NULL,
  `fetchTime` double DEFAULT NULL,
  `id` int NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `posts`
--

LOCK TABLES `posts` WRITE;
/*!40000 ALTER TABLE `posts` DISABLE KEYS */;
INSERT INTO `posts` VALUES ('NG','https://www.nationalgeographic.com/environment/article/artificial-night-sky-light-pollution-trees-insects','https://i.natgeofe.com/n/6d77acf5-8694-493c-8477-1834b7c560ac/NationalGeographic_1215495.jpg','One surprising way humans are disrupting nature? Streetlights',20240915004213.043,1),('NG','https://www.nationalgeographic.com/environment/article/corn-sweat-warming-hole-hurricane-drought','https://i.natgeofe.com/n/8b3da326-6751-49a0-8749-9abf3e9a4317/GettyImages-2163447631.jpg','‘Corn sweat’—and other weird weather phenomena—explained',20240915004305.45,2),('NG','https://www.nationalgeographic.com/environment/article/wildfire-smoke-lake-tahoe-climate','https://i.natgeofe.com/n/4ecf8b15-72b3-4496-9418-72320123d5b5/NationalGeographic_2799566.jpg','Here\'s how wildfire smoke impacts lakes',20240915004305.453,3),('NG','https://www.nationalgeographic.com/environment/article/waterspouts-tornado-mediterranean-yacht-climate','https://i.natgeofe.com/n/c00608f8-6d21-4951-a84a-d919d1ab964c/h_27.RTS2P5QS.jpg','A sea tornado sank a yacht. We might see them more often.',20240915004305.457,4),('NG','https://www.nationalgeographic.com/science/article/ocean-x-exploration-whales-sharks','https://i.natgeofe.com/n/4c46e097-b5b1-46fb-9406-e98093fb7839/STOCK_NGSF000003_190625__67259.jpg','How billions of dollars are revolutionizing ocean exploration',20240915004305.46,5),('NG','https://www.nationalgeographic.com/travel/article/paid-content-stargazing-chile','https://i.natgeofe.com/n/a371dd29-9ae2-463b-99dd-4fbd0bac0b76/GettyImages-2151497266.gif','Where to go stargazing in Chile according to a local astronomer',20240915004305.46,6),('NG','https://www.nationalgeographic.com/environment/article/tela-bay-coral-reef-mystery','https://i.natgeofe.com/n/9d65ec4d-fdfc-4867-80a3-8ebd35cb3397/Honduras_UW_reef_Great-star-coral_P1133568.jpg','This coral reef should be dead—so why is it thriving?',20240915004305.465,7),('NG','https://www.nationalgeographic.com/environment/article/critical-issues-marine-pollution','https://i.natgeofe.com/n/87d6217b-d811-4c1b-8296-8e59195388b2/16647.jpg','Marine pollution, explained',20240915004305.47,8),('NG','https://www.nationalgeographic.com/environment/article/how-to-avoid-microplastic-health-home','https://i.natgeofe.com/n/67aa9308-e924-4571-992f-f00ee5d5407d/GettyImages-1040124206.jpg','Where are microplastics found in your home?',20240915004305.473,9),('NG','https://www.nationalgeographic.com/environment/article/maui-fire-lahaina-survivors-update','https://i.natgeofe.com/n/3bb566b5-4aa6-4110-82d8-3b176dcd190f/AP24179194547185.jpg','Inside the Native Hawaiian push to rebuild Maui after wildfires',20240915004305.477,10),('NG','https://www.nationalgeographic.com/environment/article/artificial-night-sky-light-pollution-trees-insects','https://i.natgeofe.com/n/6d77acf5-8694-493c-8477-1834b7c560ac/NationalGeographic_1215495.jpg','One surprising way humans are disrupting nature? Streetlights',20240915004305.477,11),('NG','https://www.nationalgeographic.com/science/article/flood-maps-fema-risk-insurance','https://i.natgeofe.com/n/b10ac51f-031f-46c7-a686-76995a6b8c37/h_16259318.jpg','Many are buying homes in flood zones—and don\'t realize it',20241027021717.805,12),('NG','https://www.nationalgeographic.com/environment/article/new-lab-to-simulate-200-mile-per-hour-hurricanes-to-make-storm-resistant-homes','https://i.natgeofe.com/n/3042f9b2-9d0f-4845-a095-b28827aca95b/7873654960_74a063b8b5_k.jpg','Should we be preparing for Category 6 hurricanes?',20241027021717.836,13),('NG','https://www.nationalgeographic.com/environment/article/spooky-lake-month-tiktok-geology-history','https://i.natgeofe.com/n/c9c0af98-9808-439b-87a4-8dd262053da9/GettyImages-522618910.jpg','5 of the world\'s spookiest lakes—and the science behind them',20241027021717.84,14),('NG','https://www.nationalgeographic.com/environment/article/rewilding-europe-national-park-romania','https://i.natgeofe.com/n/6b707266-7bc4-429d-90ad-168e9445fe4f/MM9089_20240526_023949.jpg','Inside the bold effort to rewild Europe',20241027021717.84,15),('NG','https://www.nationalgeographic.com/environment/article/hurricanes-rapid-intensification-climate-change','https://i.natgeofe.com/n/330269cc-c5c2-428c-a8c1-34a4e73c45a9/AP23297685383196.jpg','Why hurricanes are getting more powerful—more quickly',20241027021717.844,16),('NG','https://www.nationalgeographic.com/environment/article/hurricane-storms-death-toll-counts','https://i.natgeofe.com/n/7a013ad4-3651-4bb3-aa50-49df2c96c27e/GettyImages-2175025563.jpg','Helene’s death toll could miss thousands of related deaths',20241027021717.848,17),('NG','https://www.nationalgeographic.com/environment/article/air-pollution','https://i.natgeofe.com/n/84765010-db5a-4c1e-b783-7b2b440a32e4/177.jpg','How air pollution affects your health',20241027021717.85,18),('NG','https://www.nationalgeographic.com/environment/article/mississippi-river-drought-climate-change-shipping','https://i.natgeofe.com/n/85bd0a19-8ac6-4965-b943-5d570f23806d/MM10178_231215_00101.jpg','Millions depend on the Mississippi River—but it\'s running dry',20241027021717.855,19),('NG','https://www.nationalgeographic.com/environment/article/why-recycling-plastic-doesnt-always-get-recycled','https://i.natgeofe.com/n/7b9d110c-dda2-4714-9ec9-d404cd37b30c/NationalGeographic_2702851.jpg','Why only a tiny fraction of your plastic actually gets recycled',20241027021717.86,20),('NG','https://www.nationalgeographic.com/environment/article/plastic-pollution','https://i.natgeofe.com/n/a2d236d8-1e10-4768-8c0d-d51847d7b6d7/01_plastic_nationalgeographic_2702698.jpg','The world\'s plastic pollution crisis, explained',20241027021717.86,21);
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

-- Dump completed on 2024-11-05 22:23:49
