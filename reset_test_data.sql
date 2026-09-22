-- ============================================================
-- I-Wish — Demo / Test Data
-- Run AFTER sql/schema.sql. Safe to re-run any time before a demo:
-- it wipes users/friends/wishlist/contributions/notifications and
-- reloads a clean, ready-to-demo state. The `items` catalog from
-- schema.sql is left untouched.
--
-- All test users share the password: 1234
-- Note: 6 users, 5 catalog items — Rokaya has no wish list item
-- pre-loaded, so you can add one live in the demo to show that flow.
-- ============================================================

USE iwish_db;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE notifications;
TRUNCATE TABLE contributions;
TRUNCATE TABLE wishlist_items;
TRUNCATE TABLE friends;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- USERS  (password for all of them is: 1234)
-- hash = SHA-256("1234")
-- ------------------------------------------------------------
INSERT INTO users (username, password, email) VALUES
    ('sarah',   '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'sarah@iwish.test'),
    ('lara',    '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'lara@iwish.test'),
    ('menna',   '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'menna@iwish.test'),
    ('jasmine', '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'jasmine@iwish.test'),
    ('fatima',  '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'fatima@iwish.test'),
    ('rokaya',  '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4', 'rokaya@iwish.test');

-- ------------------------------------------------------------
-- FRIENDS  (everyone is already mutual friends — no pending
-- requests needed to start the demo, though you can still send
-- new ones live to show that flow off too)
-- One row per pair is enough: areFriends()/getFriends() both
-- check either direction.
-- ------------------------------------------------------------
INSERT INTO friends (user_id, friend_id, status)
SELECT a.id, b.id, 'ACCEPTED'
FROM users a JOIN users b ON a.username < b.username;

-- ------------------------------------------------------------
-- WISHLIST ITEMS  (one per user, price copied from the catalog)
-- Rokaya has no item pre-loaded — add one live in the demo.
-- ------------------------------------------------------------
INSERT INTO wishlist_items (user_id, item_id, price, amount_raised, is_completed)
SELECT u.id, i.id, i.default_price, 0, FALSE
FROM users u JOIN items i
  ON (u.username = 'sarah'   AND i.name = 'Wireless Headphones')
  OR (u.username = 'lara'    AND i.name = 'Coffee Maker')
  OR (u.username = 'menna'   AND i.name = 'Novel: Dune')
  OR (u.username = 'jasmine' AND i.name = 'Running Shoes')
  OR (u.username = 'fatima'  AND i.name = 'Board Game Night Kit');

-- ------------------------------------------------------------
-- CONTRIBUTIONS
-- Scenario 1 — Sarah's headphones (89.99): lara + menna fully fund
-- it, so the demo already has one COMPLETED item with a
-- notification on both sides ready to show in the Notifications screen.
-- Scenario 2 — Lara's coffee maker (49.99): menna partially
-- contributes, so the demo also has an in-progress item to
-- contribute the rest to live.
-- ------------------------------------------------------------
INSERT INTO contributions (wishlist_item_id, contributor_id, amount)
SELECT wi.id, u.id, 50.00
FROM wishlist_items wi JOIN users owner ON wi.user_id = owner.id
JOIN users u ON u.username = 'lara'
WHERE owner.username = 'sarah';

INSERT INTO contributions (wishlist_item_id, contributor_id, amount)
SELECT wi.id, u.id, 39.99
FROM wishlist_items wi JOIN users owner ON wi.user_id = owner.id
JOIN users u ON u.username = 'menna'
WHERE owner.username = 'sarah';

UPDATE wishlist_items wi
JOIN users owner ON wi.user_id = owner.id
SET wi.amount_raised = 89.99, wi.is_completed = TRUE
WHERE owner.username = 'sarah';

INSERT INTO contributions (wishlist_item_id, contributor_id, amount)
SELECT wi.id, u.id, 20.00
FROM wishlist_items wi JOIN users owner ON wi.user_id = owner.id
JOIN users u ON u.username = 'menna'
WHERE owner.username = 'lara';

UPDATE wishlist_items wi
JOIN users owner ON wi.user_id = owner.id
SET wi.amount_raised = 20.00
WHERE owner.username = 'lara';

-- ------------------------------------------------------------
-- NOTIFICATIONS  (matching what notifyItemCompleted() would have
-- written for Scenario 1, so the Notifications screen already has
-- something to show without needing to trigger it live first)
-- ------------------------------------------------------------
INSERT INTO notifications (user_id, type, message)
SELECT u.id, 'BUYER_COMPLETED',
       'The gift "Wireless Headphones" for sarah is now fully funded. Thank you for contributing!'
FROM users u WHERE u.username IN ('lara', 'menna');

INSERT INTO notifications (user_id, type, message)
SELECT u.id, 'RECEIVER_BOUGHT',
       'Your wish list item "Wireless Headphones" was bought by lara and menna!'
FROM users u WHERE u.username = 'sarah';
