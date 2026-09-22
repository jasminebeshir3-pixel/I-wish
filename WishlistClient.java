package org.example.person3;

import org.example.server.Protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Person 3 — Wish List Management
 * ---------------------------------------------------------------
 * Thin network layer that talks to the shared I-Wish server using
 * the exact request/response format defined in Protocol.java.
 *
 * One WishlistClient = one open socket. Create it once (e.g. right
 * after login) and reuse it for every wishlist screen, or create a
 * new one per screen — either works, this class doesn't care.
 *
 * All methods throw IOException if the server can't be reached, and
 * WishlistException if the server replies with ERROR|... .
 */
public class WishlistClient {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    public WishlistClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void close() {
        try { in.close(); } catch (IOException ignored) {}
        out.close();
        try { socket.close(); } catch (IOException ignored) {}
    }

    // ---------------------------------------------------------------
    // Person 3 commands
    // ---------------------------------------------------------------

    /** Catalog of items an item can be added from (admin-managed, read-only here). */
    public List<CatalogItem> getAvailableItems() throws IOException {
        String data = sendAndGetData(Protocol.GET_AVAILABLE_ITEMS);
        List<CatalogItem> result = new ArrayList<>();
        if (data.isEmpty()) return result;
        for (String record : data.split(Protocol.RECORD_SEP)) {
            String[] f = record.split(Protocol.FIELD_SEP, -1);
            // id,,name,,description,,defaultPrice
            result.add(new CatalogItem(
                    Integer.parseInt(f[0]),
                    f[1],
                    f[2],
                    Double.parseDouble(f[3])));
        }
        return result;
    }

    /** Adds a catalog item to this user's wish list at the given price. Returns the new wishlist_item id. */
    public int createWishlistItem(int userId, int itemId, double price) throws IOException {
        String data = sendAndGetData(Protocol.CREATE_WISHLIST_ITEM + "|" + userId + "|" + itemId + "|" + price);
        return Integer.parseInt(data);
    }

    /** Changes the price/target-amount of an existing wish list item. */
    public void updateWishlistItemPrice(int wishlistItemId, double newPrice) throws IOException {
        sendAndGetData(Protocol.UPDATE_WISHLIST_ITEM + "|" + wishlistItemId + "|" + newPrice);
    }

    /** Removes an item from this user's wish list. */
    public void deleteWishlistItem(int wishlistItemId) throws IOException {
        sendAndGetData(Protocol.DELETE_WISHLIST_ITEM + "|" + wishlistItemId);
    }

    /** This user's own wish list. */
    public List<WishlistItem> getMyWishlist(int userId) throws IOException {
        String data = sendAndGetData(Protocol.GET_MY_WISHLIST + "|" + userId);
        List<WishlistItem> result = new ArrayList<>();
        if (data.isEmpty()) return result;
        for (String record : data.split(Protocol.RECORD_SEP)) {
            String[] f = record.split(Protocol.FIELD_SEP, -1);
            // id,,name,,price,,amountRaised,,isCompleted
            result.add(new WishlistItem(
                    Integer.parseInt(f[0]),
                    f[1],
                    Double.parseDouble(f[2]),
                    Double.parseDouble(f[3]),
                    Boolean.parseBoolean(f[4])));
        }
        return result;
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    /** Sends one line, reads one line back, and returns the payload after "OK|" — or throws on ERROR/empty replies. */
    private String sendAndGetData(String request) throws IOException {
        out.println(request);
        String response = in.readLine();

        if (response == null) {
            throw new IOException("Server closed the connection.");
        }
        if (response.equals(Protocol.OK)) {
            return ""; // bare "OK" with no payload (e.g. UPDATE / DELETE)
        }
        if (response.startsWith(Protocol.OK + "|")) {
            return response.substring((Protocol.OK + "|").length());
        }
        if (response.startsWith(Protocol.ERROR + "|")) {
            throw new WishlistException(response.substring((Protocol.ERROR + "|").length()));
        }
        throw new WishlistException("Unexpected server response: " + response);
    }
}
