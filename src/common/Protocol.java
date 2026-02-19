package common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Protocol constants shared between server and client.
 *
 * ┌──────────────────────────────────────────────────────────┐
 * │ HOW TO ADD A NEW FACULTY: │
 * │ │
 * │ 1. Add credentials in SecurityUtil.java │
 * │ 2. Add hostname mapping below: │
 * │ HOSTNAMES.put("faculty3", "FACULTY3-PC"); │
 * │ 3. Rebuild and run! │
 * └──────────────────────────────────────────────────────────┘
 */
public final class Protocol {

    private Protocol() {
    }

    // ══════════════════════════════════════════════
    // Network Configuration
    // ══════════════════════════════════════════════

    /** TCP port used for all communication */
    public static final int PORT = 5050;

    /** Buffer size for file transfers (8 KB) */
    public static final int BUFFER_SIZE = 8192;

    /** Socket timeout in milliseconds (60 seconds) */
    public static final int SOCKET_TIMEOUT_MS = 60_000;

    // ══════════════════════════════════════════════
    // Paths
    // ══════════════════════════════════════════════

    /** The ONLY folder the server is allowed to share */
    public static final String SHARED_FOLDER = "C:\\Class Room Share Folder";

    /** Temporary download folder on the client side */
    public static final String TEMP_FOLDER = "C:\\TempClassFiles";

    // ══════════════════════════════════════════════
    // Faculty Hostname Mappings — Add new faculty here
    // ══════════════════════════════════════════════

    private static final Map<String, String> HOSTNAMES = new LinkedHashMap<>();
    static {
        // Format: HOSTNAMES.put("username", "HOSTNAME");
        // The hostname must match the actual Windows PC name.
        // Use "localhost" for local testing.

        HOSTNAMES.put("faculty1", "LAPTOP-UHBD48G0"); // Change to "FACULTY1-PC" for production
        HOSTNAMES.put("faculty2", "MrunalHPi5");
        HOSTNAMES.put("faculty3", "DESKTOP-35DPGU5");
        HOSTNAMES.put("faculty4", "FACULTY4-PC");
        HOSTNAMES.put("faculty5", "FACULTY5-PC");
        HOSTNAMES.put("02906", "02906");
        HOSTNAMES.put("02907", "02907");
        HOSTNAMES.put("02908", "02908");
        HOSTNAMES.put("02909", "02909");
        HOSTNAMES.put("02910", "02910");
        HOSTNAMES.put("02911", "02911");
        HOSTNAMES.put("02912", "02912");
        HOSTNAMES.put("02913", "02913");
        HOSTNAMES.put("02914", "02914");
        HOSTNAMES.put("02915", "02915");
        HOSTNAMES.put("02194", "02194");

        // ↑ Add more faculty hostname mappings above ↑
    }

    /**
     * Returns the hostname for a given faculty username.
     *
     * @param username the faculty username (e.g. "faculty1")
     * @return the corresponding hostname, or null if unknown
     */
    public static String getHostnameForFaculty(String username) {
        if (username == null)
            return null;
        return HOSTNAMES.get(username.trim());
    }

    /**
     * Returns all registered faculty usernames.
     */
    public static Set<String> getAllFacultyUsernames() {
        return HOSTNAMES.keySet();
    }

    /**
     * Reverse lookup — finds the faculty username whose mapped hostname
     * matches the given PC hostname (case-insensitive).
     *
     * @param hostname the current PC hostname (e.g. "HP", "LAPTOP-UHBD48G0")
     * @return the matching faculty username, or null if no match
     */
    public static String getFacultyForHostname(String hostname) {
        if (hostname == null)
            return null;
        for (Map.Entry<String, String> entry : HOSTNAMES.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(hostname.trim())) {
                return entry.getKey();
            }
        }
        return null;
    }

    // ══════════════════════════════════════════════
    // Authentication Messages
    // ══════════════════════════════════════════════

    public static final String AUTH_SUCCESS = "AUTH_SUCCESS";
    public static final String AUTH_FAILED = "AUTH_FAILED";

    // ══════════════════════════════════════════════
    // File Transfer Messages
    // ══════════════════════════════════════════════

    public static final String FILE_COUNT_PREFIX = "FILE_COUNT:";
    public static final String NO_FILES = "NO_FILES";
    public static final String FILE_INFO_PREFIX = "FILE_INFO:";
    public static final String READY = "READY";
    public static final String FILE_RECEIVED = "FILE_RECEIVED";
    public static final String FILE_ERROR = "FILE_ERROR";
    public static final String TRANSFER_COMPLETE = "TRANSFER_COMPLETE";

    // ══════════════════════════════════════════════
    // Session & Error Messages
    // ══════════════════════════════════════════════

    public static final String LOGOUT = "LOGOUT";
    public static final String ERROR_PREFIX = "ERROR:";
    public static final String DELIMITER = ":";

    // ══════════════════════════════════════════════
    // UDP Auto-Discovery
    // ══════════════════════════════════════════════

    public static final int DISCOVERY_PORT = 8888;
    public static final String DISCOVER_SERVER_REQUEST = "DISCOVER_LAN_FILE_SERVER_REQ";
    public static final String DISCOVER_SERVER_RESPONSE = "DISCOVER_LAN_FILE_SERVER_RES";
}
