package org.example;
import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket socket;
    // FIX (Person 6): each connection now gets its OWN DatabaseManager/Connection,
    // instead of every client thread sharing one Connection object. A single JDBC
    // Connection is not safe to use from multiple threads at once - with the old
    // shared "db" field, having two client windows open at the same time could
    // corrupt a ResultSet mid-read or throw random SQL errors. One connection per
    // socket removes the sharing entirely.
    private final DatabaseManager db = new DatabaseManager();
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
                String response = handleRequest(line);
                out.println(response);
            }
        } catch (IOException e) {
            System.err.println("[Server] Client disconnected: " + e.getMessage());
        } finally {
            closeQuietly();
        }
    }

    private String handleRequest(String line) {
        try {
            String[] parts = line.split(Protocol.ARG_SEP);
            String command = parts[0];

            switch (command) {

                // Person 1: Authentication
                case Protocol.REGISTER: {
                    int id = db.registerUser(parts[1], parts[2], parts[3]);
                    return id != -1 ? ok(String.valueOf(id)) : error("Registration failed");
                }
                case Protocol.LOGIN: {
                    int id = db.login(parts[1], parts[2]);
                    return id != -1 ? ok(String.valueOf(id)) : error("Invalid username or password");
                }

                // Person 2: Friends
                case Protocol.ADD_FRIEND: {
                    int userId = Integer.parseInt(parts[1]);
                    Integer friendId = db.findUserIdByUsername(parts[2]);
                    if (friendId == null) return error("User not found");
                    db.sendFriendRequest(userId, friendId);
                    return ok();
                }
                case Protocol.REMOVE_FRIEND: {
                    db.removeFriend(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    return ok();
                }
                case Protocol.ACCEPT_FRIEND: {
                    db.respondToFriendRequest(Integer.parseInt(parts[1]), true);
                    return ok();
                }
                case Protocol.DECLINE_FRIEND: {
                    db.respondToFriendRequest(Integer.parseInt(parts[1]), false);
                    return ok();
                }
                case Protocol.GET_FRIEND_REQUESTS: {
                    List<String> reqs = db.getPendingFriendRequests(Integer.parseInt(parts[1]));
                    return ok(String.join(Protocol.RECORD_SEP, reqs));
                }
                case Protocol.GET_SENT_FRIEND_REQUESTS: {
                    List<String> sent = db.getSentFriendRequests(Integer.parseInt(parts[1]));
                    return ok(String.join(Protocol.RECORD_SEP, sent));
                }
                case Protocol.CANCEL_FRIEND_REQUEST: {
                    db.cancelFriendRequest(Integer.parseInt(parts[1]));
                    return ok();
                }
                case Protocol.GET_FRIENDS: {
                    List<String> friends = db.getFriends(Integer.parseInt(parts[1]));
                    return ok(String.join(Protocol.RECORD_SEP, friends));
                }

                // Person 3: My wish list
                case Protocol.GET_AVAILABLE_ITEMS: {
                    List<String> items = db.getAvailableItems();
                    return ok(String.join(Protocol.RECORD_SEP, items));
                }
                case Protocol.CREATE_WISHLIST_ITEM: {
                    int wlId = db.createWishlistItem(
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            new BigDecimal(parts[3]));
                    return wlId != -1 ? ok(String.valueOf(wlId)) : error("Could not create item");
                }
                case Protocol.UPDATE_WISHLIST_ITEM: {
                    db.updateWishlistItemPrice(Integer.parseInt(parts[1]), new BigDecimal(parts[2]));
                    return ok();
                }
                case Protocol.DELETE_WISHLIST_ITEM: {
                    db.deleteWishlistItem(Integer.parseInt(parts[1]));
                    return ok();
                }
                case Protocol.GET_MY_WISHLIST: {
                    List<String> list = db.getWishlist(Integer.parseInt(parts[1]));
                    return ok(String.join(Protocol.RECORD_SEP, list));
                }

                // Person 4: Friend's wish list
                case Protocol.GET_FRIEND_WISHLIST: {
                    // FIX (Person 6): now requires the caller's own id (parts[1]) too.
                    // Before, ANY userId could be passed as the "friend" and this would
                    // happily return their full wish list with no relationship check -
                    // now it 403s unless the two are actually accepted friends.
                    int myUserId = Integer.parseInt(parts[1]);
                    int friendUserId = Integer.parseInt(parts[2]);
                    if (!db.areFriends(myUserId, friendUserId)) {
                        return error("You can only view a friend's wish list.");
                    }
                    List<String> list = db.getWishlist(friendUserId);
                    return ok(String.join(Protocol.RECORD_SEP, list));
                }

                // Person 5: Contributions & notifications
                case Protocol.CONTRIBUTE: {
                    boolean completed = db.contribute(
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            new BigDecimal(parts[3]));
                    return ok(String.valueOf(completed));
                }
                case Protocol.GET_NOTIFICATIONS: {
                    List<String> notifs = db.getNotifications(Integer.parseInt(parts[1]));
                    return ok(String.join(Protocol.RECORD_SEP, notifs));
                }
                case Protocol.MARK_NOTIFICATION_READ: {
                    db.markNotificationRead(Integer.parseInt(parts[1]));
                    return ok();
                }

                default:
                    return error("Unknown command: " + command);
            }
        } catch (SQLException e) {
            return error("Database error: " + e.getMessage());
        } catch (Exception e) {
            return error("Bad request: " + e.getMessage());
        }
    }

    private String ok() {
        return Protocol.OK;
    }

    private String ok(String data) {
        return Protocol.OK + "|" + data;
    }

    private String error(String message) {
        return Protocol.ERROR + "|" + message;
    }

    private void closeQuietly() {
        db.disconnect();
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }
}
