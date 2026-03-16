CREATE DATABASE IF NOT EXISTS `orderdb`;
CREATE DATABASE IF NOT EXISTS `inventorydb`;
CREATE DATABASE IF NOT EXISTS `paymentdb`;

GRANT ALL PRIVILEGES ON *.* TO 'root'@'%';
FLUSH PRIVILEGES;

USE `inventorydb`;
CREATE TABLE IF NOT EXISTS `inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `available_quantity` int DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Insert default 1000 item product 1
INSERT INTO `inventory` (`product_id`, `available_quantity`) VALUES (1, 1000) ON DUPLICATE KEY UPDATE `available_quantity`=1000;
