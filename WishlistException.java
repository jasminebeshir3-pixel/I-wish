package org.example.person3;

/** Thrown when the server replies with "ERROR|message" to a wishlist command. */
public class WishlistException extends RuntimeException {
    public WishlistException(String message) {
        super(message);
    }
}
