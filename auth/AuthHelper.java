package auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat; // Requires Java 17+; for older Java, use a library or manual hex conversion

/**
 * Simple utility for password hashing.
 * No cookie logic needed for session-based auth.
 */
public class AuthHelper {

    /**
     * Hashes a password using SHA-256.
     * @param password The plain text password.
     * @return The SHA-256 hash as a hexadecimal string.
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
             System.err.println("Attempted to hash null or empty password.");
             // Return a constant invalid hash or throw an exception
             // Returning a known invalid hash might be slightly safer against timing attacks
             // if the calling code doesn't handle nulls properly.
             return "invalid_hash_empty_password_provided";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            // Requires Java 17+ for HexFormat. If using older Java, implement hex conversion manually.
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // This should realistically never happen with SHA-256
            System.err.println("CRITICAL: SHA-256 algorithm not found!");
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    // Manual Hex Conversion (Example for Java < 17)
    /*
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    // If using manual conversion, replace HexFormat.of().formatHex(hash) with bytesToHex(hash)
    */

}