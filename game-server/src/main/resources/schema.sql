-- GameGroup Database Schema
-- Run this script to initialize the database

CREATE DATABASE IF NOT EXISTS gamegroup DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE gamegroup;

-- User table
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    openid VARCHAR(64) NOT NULL UNIQUE,
    union_id VARCHAR(64),
    nickname VARCHAR(64),
    avatar_url VARCHAR(512),
    phone VARCHAR(20),
    off_work_hour INT DEFAULT 18,
    off_work_minute INT DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Game table
CREATE TABLE IF NOT EXISTS t_game (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    icon VARCHAR(512),
    description VARCHAR(1024),
    rules TEXT,
    min_players INT NOT NULL DEFAULT 2,
    max_players INT NOT NULL DEFAULT 2,
    status TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Match room table
CREATE TABLE IF NOT EXISTS t_match_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id BIGINT NOT NULL,
    verify_code VARCHAR(8) NOT NULL UNIQUE,
    creator_id BIGINT NOT NULL,
    player_ids VARCHAR(512),
    current_players INT NOT NULL DEFAULT 1,
    max_players INT NOT NULL DEFAULT 2,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_time DATETIME NOT NULL,
    INDEX idx_verify_code (verify_code),
    INDEX idx_game_id (game_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Box table
CREATE TABLE IF NOT EXISTS t_box (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    width INT NOT NULL,
    height INT NOT NULL,
    total_value DECIMAL(12,2) NOT NULL,
    starting_price DECIMAL(12,2) NOT NULL,
    fill_rate DECIMAL(5,2) NOT NULL,
    revealed_qualities VARCHAR(256) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Box item table
CREATE TABLE IF NOT EXISTS t_box_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    box_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    quality VARCHAR(20) NOT NULL,
    value DECIMAL(12,2) NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    position_x INT NOT NULL,
    position_y INT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_box_item_box_id (box_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Auction table
CREATE TABLE IF NOT EXISTS t_auction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    box_id BIGINT NOT NULL,
    current_round INT NOT NULL DEFAULT 1,
    round_end_time DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    winner_id BIGINT,
    final_price DECIMAL(12,2),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_auction_room_id (room_id),
    INDEX idx_auction_box_id (box_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Auction bid table
CREATE TABLE IF NOT EXISTS t_auction_bid (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    round INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALID',
    bid_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auction_bid_auction_id (auction_id),
    INDEX idx_auction_bid_user_id (user_id),
    UNIQUE KEY uk_auction_round_user (auction_id, round, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Skill card definition table
CREATE TABLE IF NOT EXISTS t_skill_card (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(256) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User skill card table
CREATE TABLE IF NOT EXISTS t_user_skill_card (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    card_code VARCHAR(64) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_skill_user_id (user_id),
    INDEX idx_user_skill_card_code (card_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Toilet session table (当前计时会话)
CREATE TABLE IF NOT EXISTS t_toilet_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    monthly_salary DECIMAL(10,2) NOT NULL DEFAULT 10000.00,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Toilet record table (历史记录)
CREATE TABLE IF NOT EXISTS t_toilet_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    duration_seconds INT NOT NULL,
    earned_money DECIMAL(10,2) NOT NULL,
    monthly_salary DECIMAL(10,2) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_start_time (start_time),
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ledger account table
CREATE TABLE IF NOT EXISTS t_ledger_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) DEFAULT 'General',
    balance DECIMAL(12,2) DEFAULT 0.00,
    icon VARCHAR(100),
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ledger_account_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ledger category table
CREATE TABLE IF NOT EXISTS t_ledger_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    type INT NOT NULL COMMENT '1-expense, 2-income',
    icon VARCHAR(100),
    sort_order INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ledger_category_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ledger record table
CREATE TABLE IF NOT EXISTS t_ledger_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    type INT NOT NULL COMMENT '1-expense, 2-income',
    record_time DATETIME NOT NULL,
    remark VARCHAR(255),
    category_name VARCHAR(50),
    category_icon VARCHAR(100),
    account_name VARCHAR(50),
    -- When this record is generated from a recurring rule, these fields are set (for idempotency / trace)
    recurring_rule_id BIGINT NULL,
    recurring_month CHAR(7) NULL COMMENT 'yyyy-MM',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, record_time),
    INDEX idx_account (account_id),
    INDEX idx_category (category_id),
    UNIQUE KEY uniq_recurring_rule_month (recurring_rule_id, recurring_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Ledger recurring rule table
-- Used for monthly recurring expense/income (currently only monthly)
CREATE TABLE IF NOT EXISTS t_ledger_recurring_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    type INT NOT NULL COMMENT '1-expense, 2-income',
    remark VARCHAR(255),
    start_month CHAR(7) NOT NULL COMMENT 'yyyy-MM',
    end_month CHAR(7) NULL COMMENT 'yyyy-MM, NULL means forever',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED',
    last_generated_month CHAR(7) NULL COMMENT 'yyyy-MM',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule_user_status (user_id, status),
    INDEX idx_rule_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample games
INSERT INTO t_game (name, icon, description, rules, min_players, max_players, status, sort_order) VALUES
('猜数字', '/images/game-number.png', '经典猜数字游戏，看谁先猜中对方的数字', '1. 双方各想一个4位数字\n2. 轮流猜测对方的数字\n3. 系统会提示几个数字正确、位置正确', 2, 2, 1, 1),
('石头剪刀布', '/images/game-stone.png', '简单有趣的石头剪刀布对战', '1. 双方同时出拳\n2. 石头胜剪刀，剪刀胜布，布胜石头\n3. 三局两胜', 2, 2, 1, 2),
('成语接龙', '/images/game-chengyu.png', '考验词汇量的成语接龙游戏', '1. 系统给出一个成语\n2. 玩家需要接龙，接不上或超时则输', 2, 4, 1, 3);

-- Insert default ledger categories (system level user_id = 0)
INSERT INTO t_ledger_category (user_id, name, type, icon, sort_order)
SELECT 0, '餐饮', 1, 'food', 1
WHERE NOT EXISTS (
    SELECT 1 FROM t_ledger_category WHERE user_id = 0 AND name = '餐饮' AND type = 1
);

INSERT INTO t_ledger_category (user_id, name, type, icon, sort_order)
SELECT 0, '交通', 1, 'transport', 2
WHERE NOT EXISTS (
    SELECT 1 FROM t_ledger_category WHERE user_id = 0 AND name = '交通' AND type = 1
);

INSERT INTO t_ledger_account (user_id, name, type, balance, icon, sort_order)
SELECT 0, '现金', 'Cash', 0.00, 'cash', 1
WHERE NOT EXISTS (
    SELECT 1 FROM t_ledger_account WHERE user_id = 0 AND name = '现金'
);
