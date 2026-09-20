package org.example;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Person 6 — minimal SHA-256 password hashing (no external dependency needed).
 * Not a replacement for a real KDF like bcrypt in production, but a lot better
 * than the plain-text passwords the schema/DB layer had before.
 */
public class PasswordUtil {

    private PasswordUtil() {}

    public static String hash(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean matches(String plainPassword, String storedHash) {
        return hash(plainPassword).equals(storedHash);
    }
}
