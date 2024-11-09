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
  `isRu` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=140 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `posts`
--

LOCK TABLES `posts` WRITE;
/*!40000 ALTER TABLE `posts` DISABLE KEYS */;
INSERT INTO `posts` VALUES ('National Geographic','https://www.nationalgeographic.com/environment/article/private-jet-flights-climate-change','https://i.natgeofe.com/n/52453337-44fe-4233-ad58-1ba02943d341/h_15714645.jpg','Private jets are increasingly replacing car trips, Read',20241109221055.64,122,0),('National Geographic','https://www.nationalgeographic.com/environment/article/hurricanes-climate-change-intensification','https://i.natgeofe.com/n/197d78cf-a39e-4f70-b14e-b99df000a6ee/GettyImages-2176954319.jpg','Are we approaching the scientific limit to a hurricane\'s power?, Read',20241109221055.645,123,0),('National Geographic','https://www.nationalgeographic.com/environment/article/arctic-research-station-women-scientists','https://i.natgeofe.com/n/6181b30b-d7b8-47b5-89ee-66dd0879a47c/STOCKPKG_MM10141_WomenofArcticScience_EstherHorvath_010.jpg','This Arctic boomtown is led by women studying climate change, Read',20241109221055.645,124,0),('National Geographic','https://www.nationalgeographic.com/science/article/ai-effects-questions-concerns','https://i.natgeofe.com/n/56f459a2-43eb-48aa-ad1e-8e240cfb61e7/NATGEO_ARTWORK_NEW_COLOR.jpg','Your biggest AI questions, answered, Read',20241109221055.65,125,0),('National Geographic','https://www.nationalgeographic.com/science/article/ai-predict-earthquakes-seismology','https://i.natgeofe.com/n/d95f57f1-f045-45b2-8aca-293f41958c36/MM10211_20240721_2304.jpg','AI is helping to find the next monster earthquake, Read',20241109221055.652,126,0),('National Geographic','https://www.nationalgeographic.com/environment/article/global-warming-effects','https://i.natgeofe.com/n/48597c14-b468-42ec-a2fa-1ff99195aed0/GettyImages-1354065189.jpg','How global warming is disrupting life on Earth, Read',20241109221055.652,127,0),('National Geographic','https://www.nationalgeographic.com/environment/article/ghostly-sightings-natural-disasters','https://i.natgeofe.com/n/dd46857a-05d4-486e-b607-a2693d7caf25/h_14002973.jpg','Why natural disasters are linked to paranormal beliefs, Read',20241109221055.656,128,0),('National Geographic','https://www.nationalgeographic.com/science/article/flood-maps-fema-risk-insurance','https://i.natgeofe.com/n/b10ac51f-031f-46c7-a686-76995a6b8c37/h_16259318.jpg','Many are buying homes in flood zones—and don\'t realize it, Read',20241109221055.656,129,0),('National Geographic','https://www.nationalgeographic.com/environment/article/new-lab-to-simulate-200-mile-per-hour-hurricanes-to-make-storm-resistant-homes','https://i.natgeofe.com/n/3042f9b2-9d0f-4845-a095-b28827aca95b/7873654960_74a063b8b5_k.jpg','Should we be preparing for Category 6 hurricanes?, Read',20241109221055.66,130,0),('National Geographic','https://www.nationalgeographic.com/science/article/dark-sky-health-benefits','https://i.natgeofe.com/n/5b749f51-334d-46fe-b56b-29f8c1cce09c/Spiti-Valley-India.jpg','Why dark skies are actually good for your health, Read',20241109221055.664,131,0),('National Geographic','https://www.nationalgeographic.com/science/article/monarch-butterflies-oyamel-forests-migration','https://i.natgeofe.com/n/08147c8b-bd8b-4305-84e4-11efc6700865/003_NGS_73252S_20_230227_0048002.jpg','To save monarchs, these scientists want to move mountains, Read',20241109221055.664,132,0),('Российское экологическое общество','https://www.ecosociety.ru/news/v-smolnom-proshlo-sovmestnoe-zasedanie-ekologicheskogo-soveta-pri-gubernatore-sankt-peterburga-i-obshhestvennogo-ekologicheskogo-soveta-pri-gubernatore-lenoblasti/','https://www.ecosociety.ru/wp-content/uploads/2024/08/2208242-1-1024x682.jpg','В Смольном прошло совместное заседание Экологического совета при губернаторе Санкт-Петербурга и Общественного экологического совета при губернаторе Ленобласти',20241109221055.668,133,1),('Российское экологическое общество','https://www.ecosociety.ru/news/glava-rossijskogo-ekologicheskogo-obshhestva-utverzhden-predsedatelem-obshhestvenno-ekspertnogo-soveta-po-natsproektu-ekologicheskoe-blagopoluchie/','https://www.ecosociety.ru/wp-content/uploads/2024/08/210824-1024x682.jpg','Глава Российского экологического общества утвержден председателем Общественно-экспертного совета по нацпроекту «Экологическое благополучие»',20241109221055.67,134,1),('Российское экологическое общество','https://www.ecosociety.ru/news/rashid-ismailov-prinyal-uchastie-v-stratsessii-po-natsproektu-ekologicheskoe-blagopoluchie/','https://www.ecosociety.ru/wp-content/uploads/2024/07/3007243-1-1024x510.png','Рашид Исмаилов принял участие в стратсессии по нацпроекту «Экологическое благополучие»',20241109221055.676,135,1),('Российское экологическое общество','https://www.ecosociety.ru/news/rashid-ismailov-ni-odin-investor-ne-dolzhen-zajti-v-lenoblast-ne-ponimaya-ekologicheskuyu-otvetstvennost/','https://www.ecosociety.ru/wp-content/uploads/2024/07/160724.jpg','Рашид Исмаилов: Ни один инвестор не должен зайти в Ленобласть, не понимая экологическую ответственность',20241109221055.68,136,1),('Российское экологическое общество','https://www.ecosociety.ru/news/unikalnyj-tsvet-i-prozrachnost-za-chto-tsenyatsya-rossijskie-izumrudy-i-kakovy-ih-zapasy/','https://www.ecosociety.ru/wp-content/uploads/2024/11/0811243-1024x565.jpg','Уникальный цвет и прозрачность: за что ценятся российские изумруды и каковы их запасы',20241109221055.68,137,1),('Российское экологическое общество','https://www.ecosociety.ru/news/anons-serii-diskussij-o-grinvoshinge-ekologicheskaya-otvetstvennost-ili-zelyonyj-kamuflyazh/','https://www.ecosociety.ru/wp-content/uploads/2024/11/0811242-1024x576.jpg','Анонс серии дискуссий о гринвошинге: Экологическая ответственность или зелёный камуфляж?',20241109221055.684,138,1),('Российское экологическое общество','https://www.ecosociety.ru/news/predsedatel-tomskogo-otdeleniya-rossijskogo-ekologicheskogo-obshhestva-vystupila-na-vserossijskoj-konferentsii-po-ekologicheskomu-obrazovaniyu/','https://www.ecosociety.ru/wp-content/uploads/2024/11/081124-1024x768.jpg','Председатель Томского отделения Российского экологического общества выступила на Всероссийской конференции по экологическому образованию',20241109221055.688,139,1);
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

-- Dump completed on 2024-11-09 23:11:37
