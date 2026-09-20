package org.example;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String URL = "jdbc:mysql://localhost:3306/iwish_db";
    // Can be overridden with the IWISH_DB_USER / IWISH_DB_PASS environment variables
    // so the password doesn't have to be committed to GitHub.
    private static final String DB_USER = System.getenv().getOrDefault("IWISH_DB_USER", "root");
    private static final String DB_PASS = System.getenv().getOrDefault("IWISH_DB_PASS", "your_password_here");
    private Connection connection;

    public DatabaseManager() {
        connect();
    }

    // Connection
    public void connect() {
        try {
            connection = DriverManager.getConnection(URL, DB_USER, DB_PASS);
            System.out.println("[DB] Connected successfully.");
        } catch (SQLException e) {
            System.err.println("[DB] Connection failed: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            System.err.println("[DB] Error closing connection: " + e.getMessage());
        }
    }

    /**
     * FIX (Person 6): the text protocol splits on "|", and list responses join
     * rows/fields with ";;" / ",,". If a username/email/password contained any
     * of those, every command using that value would silently mis-parse.
     * Reject them up front instead of corrupting the wire format later.
     */
    private void checkNoReservedChars(String value, String fieldName) throws SQLException {
        if (value == null || value.contains("|") || value.contains(",,") || value.contains(";;")) {
            throw new SQLException(fieldName + " can't contain '|', ',,' or ';;'.");
        }
    }

    // Person 1: Authentication
    public int registerUser(String username, String password, String email) throws SQLException {
        checkNoReservedChars(username, "Username");
        checkNoReservedChars(password, "Password");
        checkNoReservedChars(email, "Email");

        String sql = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            // FIX (Person 6): was stored in plain text before - now SHA-256 hashed.
            ps.setString(2, PasswordUtil.hash(password));
            ps.setString(3, email);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    //Returns the user id if credentials match, or -1 if invalid.
    public int login(String username, String password) throws SQLException {
        String sql = "SELECT id, password FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                // FIX (Person 6): compare against the stored hash, not a plain-text match.
                if (rs.next() && PasswordUtil.matches(password, rs.getString("password"))) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    public Integer findUserIdByUsername(String username) throws SQLException {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return null;
    }

    // Person 2: Friends system
    public void sendFriendRequest(int userId, int friendId) throws SQLException {
        if (userId == friendId) {
            throw new SQLException("You can't add yourself as a friend.");
        }
        if (areFriends(userId, friendId)) {
            throw new SQLException("You're already friends.");
        }
        // FIX (Person 6): a plain INSERT here meant that once a request had been
        // DECLINED (or CANCELLED and re-sent), sending it again hit the
        // UNIQUE(user_id, friend_id) constraint and blew up with a raw SQL error.
        // Re-sending now just resets that same row back to PENDING.
        String sql = "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, 'PENDING') " +
                "ON DUPLICATE KEY UPDATE status = 'PENDING', created_at = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, friendId);
            ps.executeUpdate();
        }
    }

    public void respondToFriendRequest(int requestId, boolean accept) throws SQLException {
        String sql = "UPDATE friends SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, accept ? "ACCEPTED" : "DECLINED");
            ps.setInt(2, requestId);
            ps.executeUpdate();
        }
    }

    public void removeFriend(int userId, int friendId) throws SQLException {
        String sql = "DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, friendId);
            ps.setInt(3, friendId);
            ps.setInt(4, userId);
            ps.executeUpdate();
        }
    }

    /**
     * NEW (Person 6): true if userA and userB are accepted friends, either direction.
     * Used to authorize GET_FRIEND_WISHLIST and CONTRIBUTE so a user can't view or
     * pay toward a stranger's wish list.
     */
    public boolean areFriends(int userA, int userB) throws SQLException {
        String sql = "SELECT 1 FROM friends WHERE status = 'ACCEPTED' " +
                "AND ((user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?))";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ps.setInt(3, userB);
            ps.setInt(4, userA);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Returns rows: requestId,,senderUsername,,senderEmail,,createdAt */
    public List<String> getPendingFriendRequests(int userId) throws SQLException {
        List<String> results = new ArrayList<>();
        String sql = "SELECT f.id, u.username, u.email, f.created_at FROM friends f " +
                "JOIN users u ON f.user_id = u.id " +
                "WHERE f.friend_id = ? AND f.status = 'PENDING'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getInt("id") + Protocol.FIELD_SEP + rs.getString("username")
                            + Protocol.FIELD_SEP + rs.getString("email")
                            + Protocol.FIELD_SEP + rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
        }
        return results;
    }

    /** Returns rows: requestId,,receiverUsername,,receiverEmail,,createdAt (requests this user sent, still pending) */
    public List<String> getSentFriendRequests(int userId) throws SQLException {
        List<String> results = new ArrayList<>();
        String sql = "SELECT f.id, u.username, u.email, f.created_at FROM friends f " +
                "JOIN users u ON f.friend_id = u.id " +
                "WHERE f.user_id = ? AND f.status = 'PENDING'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getInt("id") + Protocol.FIELD_SEP + rs.getString("username")
                            + Protocol.FIELD_SEP + rs.getString("email")
                            + Protocol.FIELD_SEP + rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
        }
        return results;
    }

    /** Cancels a request the current user sent (only while it's still pending). */
    public void cancelFriendRequest(int requestId) throws SQLException {
        String sql = "DELETE FROM friends WHERE id = ? AND status = 'PENDING'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ps.executeUpdate();
        }
    }

    /** Returns rows: friendId,,username,,email (accepted friends only, either direction) */
    public List<String> getFriends(int userId) throws SQLException {
        List<String> results = new ArrayList<>();
        String sql = "SELECT u.id, u.username, u.email FROM friends f " +
                "JOIN users u ON u.id = CASE WHEN f.user_id = ? THEN f.friend_id ELSE f.user_id END " +
                "WHERE (f.user_id = ? OR f.friend_id = ?) AND f.status = 'ACCEPTED'";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getInt("id") + Protocol.FIELD_SEP + rs.getString("username")
                            + Protocol.FIELD_SEP + rs.getString("email"));
                }
            }
        }
        return results;
    }
    // Person 3 & 4: Wish list items (own + friends')

    //Returns rows: itemId,,name,,description,,defaultPrice
    public List<String> getAvailableItems() throws SQLException {
        List<String> results = new ArrayList<>();
        String sql = "SELECT id, name, description, default_price FROM items";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                results.add(rs.getInt("id") + Protocol.FIELD_SEP +
                        rs.getString("name") + Protocol.FIELD_SEP +
                        rs.getString("description") + Protocol.FIELD_SEP +
                        rs.getBigDecimal("default_price"));
            }
        }
        return results;
    }

    // FIX (Person 6): price/amount now BigDecimal instead of double, to avoid
    // binary floating-point rounding errors on money (e.g. the raised >= price
    // completion check below). The wire format is unaffected - it was always
    // just a decimal number as text, so no other file needed to change.
    public int createWishlistItem(int userId, int itemId, BigDecimal price) throws SQLException {
        String sql = "INSERT INTO wishlist_items (user_id, item_id, price) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, itemId);
            ps.setBigDecimal(3, price);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public void updateWishlistItemPrice(int wishlistItemId, BigDecimal newPrice) throws SQLException {
        String sql = "UPDATE wishlist_items SET price = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBigDecimal(1, newPrice);
            ps.setInt(2, wishlistItemId);
            ps.executeUpdate();
        }
    }

    public void deleteWishlistItem(int wishlistItemId) throws SQLException {
        String sql = "DELETE FROM wishlist_items WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, wishlistItemId);
            ps.executeUpdate();
        }
    }

    /** Returns rows: wishlistItemId,,itemName,,price,,amountRaised,,isCompleted */
    public List<String> getWishlist(int userId) throws SQLException {
        List<String> results = new ArrayList<>();
        String sql = "SELECT w.id, i.name, w.price, w.amount_raised, w.is_completed " +
                "FROM wishlist_items w JOIN items i ON w.item_id = i.id WHERE w.user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getInt("id") + Protocol.FIELD_SEP +
                            rs.getString("name") + Protocol.FIELD_SEP +
                            rs.getBigDecimal("price") + Protocol.FIELD_SEP +
                            rs.getBigDecimal("amount_raised") + Protocol.FIELD_SEP +
                            rs.getBoolean("is_completed"));
                }
            }
        }
        return results;
    }

    // Person 5: Contributions & notifications
    public boolean contribute(int contributorId, int wishlistItemId, BigDecimal amount) throws SQLException {
        if (amount == null || amount.signum() <= 0) {
            throw new SQLException("Contribution amount must be positive.");
        }

        String checkSql = "SELECT user_id, price, amount_raised, is_completed FROM wishlist_items WHERE id = ?";
        int receiverId;
        BigDecimal price, alreadyRaised;
        boolean alreadyCompleted;
        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setInt(1, wishlistItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Wish list item not found.");
                receiverId = rs.getInt("user_id");
                price = rs.getBigDecimal("price");
                alreadyRaised = rs.getBigDecimal("amount_raised");
                alreadyCompleted = rs.getBoolean("is_completed");
            }
        }
        // FIX (Person 6): these three checks were missing before - a user could
        // fund their own wish, or keep paying into an already-fulfilled item.
        if (contributorId == receiverId) {
            throw new SQLException("You can't contribute to your own wish list item.");
        }
        if (alreadyCompleted) {
            throw new SQLException("This item is already fully funded.");
        }
        if (!areFriends(contributorId, receiverId)) {
            throw new SQLException("You can only contribute to a friend's wish list.");
        }

        String insertSql = "INSERT INTO contributions (wishlist_item_id, contributor_id, amount) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            ps.setInt(1, wishlistItemId);
            ps.setInt(2, contributorId);
            ps.setBigDecimal(3, amount);
            ps.executeUpdate();
        }

        String updateSql = "UPDATE wishlist_items SET amount_raised = amount_raised + ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, wishlistItemId);
            ps.executeUpdate();
        }

        BigDecimal newRaised = alreadyRaised.add(amount);
        if (newRaised.compareTo(price) >= 0) {
            markWishlistItemCompleted(wishlistItemId);
            notifyItemCompleted(wishlistItemId, receiverId);
            return true;
        }
        return false;
    }

    /**
     * Spec items 8 & 9. When an item's price is fully covered:
     *  - every friend who contributed gets a "gift completed" notification (buyer side)
     *  - the owner gets a notification naming the friend(s) who bought it (receiver side)
     */
    private void notifyItemCompleted(int wishlistItemId, int receiverId) throws SQLException {
        String itemName = "your item";
        String nameSql = "SELECT i.name FROM wishlist_items w JOIN items i ON i.id = w.item_id WHERE w.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(nameSql)) {
            ps.setInt(1, wishlistItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) itemName = rs.getString(1);
            }
        }

        String receiverName = "your friend";
        try (PreparedStatement ps = connection.prepareStatement("SELECT username FROM users WHERE id = ?")) {
            ps.setInt(1, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) receiverName = rs.getString(1);
            }
        }

        List<Integer> contributorIds = new ArrayList<>();
        List<String> contributorNames = new ArrayList<>();
        String contribSql = "SELECT c.contributor_id, u.username FROM contributions c "
                + "JOIN users u ON u.id = c.contributor_id WHERE c.wishlist_item_id = ? "
                + "GROUP BY c.contributor_id, u.username ORDER BY MIN(c.created_at)";
        try (PreparedStatement ps = connection.prepareStatement(contribSql)) {
            ps.setInt(1, wishlistItemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    contributorIds.add(rs.getInt(1));
                    contributorNames.add(rs.getString(2));
                }
            }
        }

        String buyerMsg = "The gift \"" + itemName + "\" for " + receiverName
                + " is now fully funded. Thank you for contributing!";
        for (int contributorId : contributorIds) {
            notify(contributorId, "BUYER_COMPLETED", limit(buyerMsg));
        }

        String receiverMsg = "Your wish list item \"" + itemName + "\" was bought by "
                + joinNames(contributorNames) + "!";
        notify(receiverId, "RECEIVER_BOUGHT", limit(receiverMsg));
    }

    private static String joinNames(List<String> names) {
        if (names.size() == 1) return names.get(0);
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
    }

    /** notifications.message is VARCHAR(255). */
    private static String limit(String msg) {
        return msg.length() <= 255 ? msg : msg.substring(0, 252) + "...";
    }

    private void markWishlistItemCompleted(int wishlistItemId) throws SQLException {
        String sql = "UPDATE wishlist_items SET is_completed = TRUE WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, wishlistItemId);
            ps.executeUpdate();
        }
    }

    public void notify(int userId, String type, String message) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, type, message) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setString(3, message);
            ps.executeUpdate();
        }
    }

    // Returns rows: id,,type,,message,,isRead
    public List<String> getNotifications(int userId) throws SQLException {
        List<String> results = new ArrayList<>();
        String sql = "SELECT id, type, message, is_read FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(rs.getInt("id") + Protocol.FIELD_SEP +
                            rs.getString("type") + Protocol.FIELD_SEP +
                            rs.getString("message") + Protocol.FIELD_SEP +
                            rs.getBoolean("is_read"));
                }
            }
        }
        return results;
    }

    public void markNotificationRead(int notificationId) throws SQLException {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        }
    }
}
