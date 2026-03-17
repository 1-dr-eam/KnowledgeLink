DROP TABLE IF EXISTS `book_info`;

CREATE TABLE `book_info` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `seller_id` INT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `author` VARCHAR(255) DEFAULT NULL,
  `publisher` VARCHAR(255) DEFAULT NULL,
  `version` VARCHAR(64) DEFAULT NULL,
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `type` VARCHAR(64) DEFAULT NULL,
  `classify` VARCHAR(64) DEFAULT NULL,
  `sub_classify` VARCHAR(64) DEFAULT NULL,
  `note` TINYINT(1) DEFAULT 0,
  `description` TEXT,
  `status` INT DEFAULT 1,
  `avatar` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `book_info`
(`seller_id`, `name`, `author`, `publisher`, `version`, `price`, `type`, `classify`, `sub_classify`, `note`, `description`, `status`, `avatar`)
VALUES
(10001, '高等数学（上）', '同济大学数学系', '高等教育出版社', '第7版', 25.00, '教材', '理学', '数学', 1, '线性代数与微积分基础教材', 1, '["https://example.com/book1-1.jpg","https://example.com/book1-2.jpg"]'),
(10002, 'Java核心技术卷I', 'Cay S. Horstmann', '机械工业出版社', '第12版', 68.50, '非教材', '工学', '计算机', 0, 'Java语言与核心类库实践', 1, '["https://example.com/book2-1.jpg"]'),
(10003, '计算机网络', '谢希仁', '电子工业出版社', '第8版', 42.00, '教材', '工学', '计算机', 1, '网络分层与协议体系讲解', 1, '["https://example.com/book3-1.jpg","https://example.com/book3-2.jpg"]');
