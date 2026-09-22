-- ============================================================
-- I-Wish Project Database Schema (MySQL)
-- Matches iwish-integrated/src/main/java/org/example/server/DatabaseManager.java
-- exactly — every table/column name here is queried by that class.
-- ============================================================

CREATE DATABASE IF NOT EXISTS iwish_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE iwish_db;

-- ------------------------------------------------------------
-- USERS  (Person 1: registerUser, login, findUserIdByUsername)
-- password stores a SHA-256 hex hash (64 chars) via PasswordUtil
-- ------------------------------------------------------------
CREATE TABLE users (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- FRIENDS  (Person 2: sendFriendRequest, respondToFriendRequest,
-- removeFriend, areFriends, getPendingFriendRequests,
-- getSentFriendRequests, cancelFriendRequest, getFriends)
-- One row per direction of a request; re-sending after a DECLINE
-- resets the same row via ON DUPLICATE KEY UPDATE, so the
-- UNIQUE(user_id, friend_id) pair below must match that exactly.
-- ------------------------------------------------------------
CREATE TABLE friends (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    friend_id  INT NOT NULL,
    status     ENUM('PENDING', 'ACCEPTED', 'DECLINED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)   REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_pair (user_id, friend_id),
    CHECK (user_id <> friend_id)
);

CREATE INDEX idx_friends_friend_status ON friends(friend_id, status);
CREATE INDEX idx_friends_user_status   ON friends(user_id, status);

-- ------------------------------------------------------------
-- ITEMS  (Person 3/4: getAvailableItems — the admin-managed
-- catalog users pick from to build their wish list)
-- ------------------------------------------------------------
CREATE TABLE items (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    description   TEXT,
    default_price DECIMAL(10,2) NOT NULL CHECK (default_price >= 0),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- WISHLIST_ITEMS  (Person 3/4: createWishlistItem,
-- updateWishlistItemPrice, deleteWishlistItem, getWishlist)
-- price is per-user (copied from items.default_price at add time,
-- but editable); amount_raised/is_completed are updated by
-- Person 5's contribute() logic.
-- ------------------------------------------------------------
CREATE TABLE wishlist_items (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    user_id        INT NOT NULL,
    item_id        INT NOT NULL,
    price          DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    amount_raised  DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (amount_raised >= 0),
    is_completed   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE RESTRICT
);

CREATE INDEX idx_wishlist_items_user ON wishlist_items(user_id);

-- ------------------------------------------------------------
-- CONTRIBUTIONS  (Person 5: contribute(), notifyItemCompleted's
-- "GROUP BY contributor_id ... ORDER BY MIN(created_at)")
-- ------------------------------------------------------------
CREATE TABLE contributions (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    wishlist_item_id  INT NOT NULL,
    contributor_id    INT NOT NULL,
    amount            DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wishlist_item_id) REFERENCES wishlist_items(id) ON DELETE CASCADE,
    FOREIGN KEY (contributor_id)   REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_contributions_wishlist_item ON contributions(wishlist_item_id);

-- ------------------------------------------------------------
-- NOTIFICATIONS  (Person 5: notify(), getNotifications(),
-- markNotificationRead() — type is a free-text label like
-- 'BUYER_COMPLETED' / 'RECEIVER_BOUGHT', message capped at 255
-- to match the app-layer limit() truncation)
-- ------------------------------------------------------------
CREATE TABLE notifications (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    type       VARCHAR(50)  NOT NULL,
    message    VARCHAR(255) NOT NULL,
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);

-- ------------------------------------------------------------
-- Seed data: a starter catalog so the app has something to test
-- wish lists, friend requests, and contributions against.
-- ------------------------------------------------------------
INSERT INTO items (name, description, default_price) VALUES
    ('Wireless Headphones', 'Noise-cancelling over-ear headphones', 89.99),
    ('Coffee Maker',        'Drip coffee machine, 12-cup',          49.99),
    ('Novel: Dune',         'Sci-fi classic by Frank Herbert',      15.50),
    ('Running Shoes',       'Lightweight trainers, size varies',    65.00),
    ('Board Game Night Kit','A bundle of 3 popular board games',   120.00);
