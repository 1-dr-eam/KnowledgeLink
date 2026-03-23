CREATE TABLE IF NOT EXISTS forum_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(1000),
    content LONGTEXT,
    author_name VARCHAR(255),
    cover_avatar VARCHAR(500),
    label VARCHAR(100),
    type VARCHAR(50),
    visible_range VARCHAR(50) DEFAULT '公开',
    page_views INT DEFAULT 0,
    like_count INT DEFAULT 0,
    collect_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    subject VARCHAR(255),
    sub_classify VARCHAR(255),
    create_time DATETIME,
    update_time DATETIME,
    KEY idx_forum_post_user_id (user_id),
    KEY idx_forum_post_subject (subject),
    KEY idx_forum_post_sub_classify (sub_classify),
    KEY idx_forum_post_visible_range (visible_range)
);

CREATE TABLE IF NOT EXISTS forum_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(255),
    content TEXT NOT NULL,
    reply_count INT DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    KEY idx_forum_comment_post_id (post_id),
    KEY idx_forum_comment_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS forum_reply_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    reply_type VARCHAR(50),
    reply_comment_id BIGINT,
    reply_user_name VARCHAR(255),
    user_id BIGINT NOT NULL,
    user_name VARCHAR(255),
    content TEXT NOT NULL,
    create_time DATETIME,
    update_time DATETIME,
    KEY idx_forum_reply_comment_comment_id (comment_id),
    KEY idx_forum_reply_comment_reply_comment_id (reply_comment_id),
    KEY idx_forum_reply_comment_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS forum_post_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    create_time DATETIME,
    UNIQUE KEY uk_forum_post_like_user_post (user_id, post_id),
    KEY idx_forum_post_like_post_id (post_id)
);

CREATE TABLE IF NOT EXISTS forum_post_collect (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    create_time DATETIME,
    UNIQUE KEY uk_forum_post_collect_user_post (user_id, post_id),
    KEY idx_forum_post_collect_post_id (post_id)
);

CREATE TABLE IF NOT EXISTS forum_comment_like (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    create_time DATETIME,
    UNIQUE KEY uk_forum_comment_like_user_comment (user_id, comment_id),
    KEY idx_forum_comment_like_comment_id (comment_id)
);

CREATE TABLE IF NOT EXISTS forum_user_follow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    follow_user_id BIGINT NOT NULL,
    create_time DATETIME,
    UNIQUE KEY uk_forum_user_follow_user_follow (user_id, follow_user_id),
    KEY idx_forum_user_follow_follow_user_id (follow_user_id)
);

CREATE TABLE IF NOT EXISTS forum_news (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    publish_date DATE,
    click_count INT DEFAULT 0,
    summary VARCHAR(1000),
    content LONGTEXT,
    avatar VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS forum_shopping_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    shopping_address VARCHAR(500) NOT NULL,
    label VARCHAR(100),
    default_flag TINYINT(1) DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME,
    KEY idx_forum_shopping_address_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS chat_message_record (
    message_id VARCHAR(64) PRIMARY KEY,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NOT NULL,
    content TEXT,
    send_time DATETIME NOT NULL,
    `read` TINYINT(1) DEFAULT 0,
    `image` TINYINT(1) DEFAULT 0,
    KEY idx_chat_message_record_from_to (from_user_id, to_user_id),
    KEY idx_chat_message_record_send_time (send_time)
);
