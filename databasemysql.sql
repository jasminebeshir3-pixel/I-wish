-- ============================================
-- FULL RESET: clears out all the messy test data
-- (duplicate ahmed/sarah, stray friendships, etc.)
-- and rebuilds a clean, consistent set of test users.
--
-- Run this ONE TIME in MySQL Workbench, all at once.
-- Keeps the catalog (items table) untouched.
-- ============================================

USE iwish_db;

SET SQL_SAFE_UPDATES = 0;

-- ---------- Wipe everything except the item catalog ----------
DELETE FROM notifications;
DELETE FROM contributions;
DELETE FROM wishlist_items;
DELETE FROM friends;
DELETE FROM users;

-- Reset auto-increment counters so ids start clean at 1 again
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE friends AUTO_INCREMENT = 1;
ALTER TABLE wishlist_items AUTO_INCREMENT = 1;
ALTER TABLE contributions AUTO_INCREMENT = 1;
ALTER TABLE notifications AUTO_INCREMENT = 1;

-- ---------- Register the team's test users (password "1234" for everyone) ----------
INSERT INTO users (username, password, email) VALUES
('ahmed',   '1234', 'ahmed@mail.com'),
('sarah',   '1234', 'sarah@mail.com'),
('jasmine', '1234', 'jasmine@mail.com'),
('rokaya',  '1234', 'rokaya@mail.com'),
('lara',    '1234', 'lara@mail.com'),
('fatima',  '1234', 'fatima@mail.com'),
('menna',   '1234', 'menna@mail.com');

-- ---------- Make everyone friends with everyone (so any teammate can test any feature) ----------
INSERT INTO friends (user_id, friend_id, status)
SELECT u1.id, u2.id, 'ACCEPTED'
FROM users u1
JOIN users u2 ON u1.id < u2.id;

-- ---------- A couple of sample wishlist items to test with ----------
INSERT INTO wishlist_items (user_id, item_id, price)
SELECT id, 1, 500.00 FROM users WHERE username = 'jasmine'; -- PlayStation 5

INSERT INTO wishlist_items (user_id, item_id, price)
SELECT id, 2, 1000.00 FROM users WHERE username = 'lara';   -- iPhone 16

INSERT INTO wishlist_items (user_id, item_id, price)
SELECT id, 3, 25.00 FROM users WHERE username = 'fatima';   -- Book: Clean Code

INSERT INTO wishlist_items (user_id, item_id, price)
SELECT id, 4, 80.00 FROM users WHERE username = 'menna';    -- Headphones

INSERT INTO wishlist_items (user_id, item_id, price)
SELECT id, 1, 500.00 FROM users WHERE username = 'rokaya';  -- PlayStation 5

INSERT INTO wishlist_items (user_id, item_id, price)
SELECT id, 2, 1000.00 FROM users WHERE username = 'sarah';  -- iPhone 16

-- ---------- Check the final, clean result ----------
SELECT id, username, email FROM users ORDER BY id;