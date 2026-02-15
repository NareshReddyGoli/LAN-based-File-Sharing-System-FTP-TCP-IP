package common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Security utility class for the LAN File Sharing System.
 * Provides SHA-256 password hashing and credential management.
 *
 * ┌──────────────────────────────────────────────────────────┐
 * │ HOW TO ADD A NEW FACULTY: │
 * │ │
 * │ 1. Add a line in the CREDENTIALS block below: │
 * │ CREDENTIALS.put("faculty3", hashPassword("pass789"));│
 * │ │
 * │ 2. Add hostname in Protocol.java: │
 * │ HOSTNAMES.put("faculty3", "FACULTY3-PC"); │
 * │ │
 * │ 3. Rebuild: build.bat │
 * │ 4. Run: run_server.bat faculty3 │
 * └──────────────────────────────────────────────────────────┘
 */
public class SecurityUtil {

    // ══════════════════════════════════════════════
    // FACULTY CREDENTIALS — Add new faculty here
    // ══════════════════════════════════════════════

    private static final Map<String, String> CREDENTIALS = new LinkedHashMap<>();
    static {
        // Format: CREDENTIALS.put("username", hashPassword("password"));
        // Just copy-paste a new line and change the values!

        CREDENTIALS.put("faculty1", hashPassword("pass123"));
        CREDENTIALS.put("faculty2", hashPassword("pass456"));
        CREDENTIALS.put("faculty3", hashPassword("pass789"));
        CREDENTIALS.put("faculty4", hashPassword("pass321"));
        CREDENTIALS.put("faculty5", hashPassword("pass654"));

        // ↑ Add more faculty entries above ↑
    }

    // ══════════════════════════════════════════════
    // Hashing Methods
    // ══════════════════════════════════════════════

    /**
     * Hashes a plain text password using SHA-256.
     *
     * @param password the plain text password
     * @return Base64-encoded SHA-256 hash string
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not available", e);
        }
    }

    /**
     * Verifies a plain text password against a stored hash.
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        return hashPassword(password).equals(hashedPassword);
    }

    // ══════════════════════════════════════════════
    // Authentication
    // ══════════════════════════════════════════════

    /**
     * Validates a username/hash pair against the credential store.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param username       the faculty username
     * @param hashedPassword the SHA-256 hash sent by the client
     * @return true if credentials are valid
     */
    public static boolean authenticate(String username, String hashedPassword) {
        if (username == null || hashedPassword == null)
            return false;

        String storedHash = CREDENTIALS.get(username.trim());
        if (storedHash == null)
            return false;

        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
                storedHash.getBytes(), hashedPassword.getBytes());
    }

    /**
     * Checks whether a given username exists.
     */
    public static boolean isValidUsername(String username) {
        return username != null && CREDENTIALS.containsKey(username.trim());
    }

    /**
     * Returns all registered faculty usernames.
     */
    public static Set<String> getAllUsernames() {
        return CREDENTIALS.keySet();
    }
}
