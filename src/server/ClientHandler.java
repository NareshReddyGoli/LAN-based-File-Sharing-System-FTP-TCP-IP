package server;

import common.Protocol;
import common.SecurityUtil;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles a single client connection in its own thread.
 * 
 * Responsibilities:
 * 1. Authenticate the client using SHA-256 hashed credentials.
 * 2. List all files in the shared folder.
 * 3. Transfer each file using buffered streams (8 KB chunks).
 * 4. Enforce folder-level access restrictions — the client
 * can NEVER access paths outside Protocol.SHARED_FOLDER.
 * 5. Clean up resources on completion or error.
 * 
 * Protocol (per connection):
 * ← username
 * ← hashedPassword
 * → AUTH_SUCCESS | AUTH_FAILED
 * → FILE_COUNT:<n> | NO_FILES
 * for each file:
 * → FILE_INFO:<name>:<size>
 * ← READY
 * → [raw bytes]
 * ← FILE_RECEIVED | FILE_ERROR
 * → TRANSFER_COMPLETE
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final String sharedFolderPath;
    private final String facultyUsername;
    private final AtomicInteger activeClients;

    private PrintWriter out;
    private BufferedReader in;
    private String clientAddress;

    /**
     * Constructs a new ClientHandler.
     *
     * @param socket           the accepted client socket
     * @param sharedFolderPath absolute path to the shared folder
     * @param facultyUsername  the faculty this server represents
     * @param activeClients    shared counter for connection tracking
     */
    public ClientHandler(Socket socket, String sharedFolderPath,
            String facultyUsername, AtomicInteger activeClients) {
        this.socket = socket;
        this.sharedFolderPath = sharedFolderPath;
        this.facultyUsername = facultyUsername;
        this.activeClients = activeClients;
        this.clientAddress = socket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        try {
            // Set a timeout so a misbehaving client cannot block a thread forever
            socket.setSoTimeout(Protocol.SOCKET_TIMEOUT_MS);

            // Initialize I/O streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            log("Client connected: " + clientAddress);

            // ── Step 1: Authenticate ──────────────────────
            if (!authenticateClient()) {
                send(Protocol.AUTH_FAILED);
                log("Authentication FAILED for " + clientAddress);
                return;
            }
            send(Protocol.AUTH_SUCCESS);
            log("Authentication PASSED for " + clientAddress);

            // ── Step 2: Send files ────────────────────────
            sendFiles();

        } catch (SocketException e) {
            log("Client " + clientAddress + " disconnected unexpectedly: " + e.getMessage());
        } catch (IOException e) {
            log("I/O error with client " + clientAddress + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    // ──────────────────────────────────────────────
    // Authentication
    // ──────────────────────────────────────────────

    /**
     * Reads username + hashed password from the client and validates them.
     *
     * @return true if authentication succeeds
     */
    private boolean authenticateClient() throws IOException {
        // Read credentials from client
        String username = in.readLine();
        if (username == null)
            return false;
        username = username.trim();

        String hashedPassword = in.readLine();
        if (hashedPassword == null)
            return false;
        hashedPassword = hashedPassword.trim();

        log("Auth attempt — user: " + username);

        // The server only accepts its own faculty's credentials
        if (!facultyUsername.equals(username)) {
            log("Rejected: username '" + username + "' does not match this server (" + facultyUsername + ")");
            return false;
        }

        // Validate against the credential store
        return SecurityUtil.authenticate(username, hashedPassword);
    }

    // ──────────────────────────────────────────────
    // File Transfer
    // ──────────────────────────────────────────────

    /**
     * Enumerates files in the shared folder and transfers each one.
     */
    private void sendFiles() throws IOException {
        Path sharedPath = Paths.get(sharedFolderPath);

        // Safety check
        if (!Files.exists(sharedPath) || !Files.isDirectory(sharedPath)) {
            send(Protocol.ERROR_PREFIX + "Shared folder not available");
            return;
        }

        // Collect only regular files (no subdirectories)
        File[] files = sharedPath.toFile().listFiles(File::isFile);
        if (files == null || files.length == 0) {
            send(Protocol.NO_FILES);
            log("No files to send.");
            return;
        }

        // Tell the client how many files are coming
        send(Protocol.FILE_COUNT_PREFIX + files.length);
        log("Preparing to send " + files.length + " file(s)");

        int successCount = 0;

        for (File file : files) {
            // ── Send file metadata ──
            String fileInfo = Protocol.FILE_INFO_PREFIX + file.getName()
                    + Protocol.DELIMITER + file.length();
            send(fileInfo);
            log("  → " + file.getName() + " (" + formatSize(file.length()) + ")");

            // ── Wait for client READY signal ──
            String clientResponse = in.readLine();
            if (!Protocol.READY.equals(clientResponse)) {
                log("Client not ready (received: " + clientResponse + "). Aborting transfers.");
                break;
            }

            // ── Transfer the file ──
            boolean ok = transferFile(file);

            // ── Wait for client confirmation ──
            String confirm = in.readLine();
            if (ok && Protocol.FILE_RECEIVED.equals(confirm)) {
                successCount++;
            } else {
                log("Client did not confirm receipt of " + file.getName()
                        + " (response: " + confirm + ")");
                break;
            }
        }

        // Signal transfer completion
        send(Protocol.TRANSFER_COMPLETE);
        log("Transfer session complete — " + successCount + "/" + files.length + " files sent.");
    }

    /**
     * Streams a single file to the client over raw bytes.
     * Uses an 8 KB buffer for efficient large-file handling.
     *
     * @param file the file to transfer
     * @return true if the entire file was sent
     */
    private boolean transferFile(File file) {
        long bytesSent = 0;
        long fileSize = file.length();

        try (FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis, Protocol.BUFFER_SIZE)) {

            OutputStream rawOut = socket.getOutputStream();
            byte[] buffer = new byte[Protocol.BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = bis.read(buffer)) != -1) {
                rawOut.write(buffer, 0, bytesRead);
                bytesSent += bytesRead;

                // Log progress every 1 MB
                if (bytesSent % (1024 * 1024) < Protocol.BUFFER_SIZE) {
                    log("    Sent " + formatSize(bytesSent) + " / " + formatSize(fileSize));
                }
            }
            rawOut.flush();

            log("  ✓ Finished sending " + file.getName());
            return bytesSent == fileSize;

        } catch (IOException e) {
            log("  ✗ Error transferring " + file.getName() + ": " + e.getMessage());
            return false;
        }
    }

    // ──────────────────────────────────────────────
    // Cleanup
    // ──────────────────────────────────────────────

    /**
     * Closes all resources and decrements the active-client counter.
     */
    private void cleanup() {
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            log("Cleanup error: " + e.getMessage());
        } finally {
            activeClients.decrementAndGet();
            log("Client " + clientAddress + " disconnected (active: " + activeClients.get() + ")");
        }
    }

    // ──────────────────────────────────────────────
    // Utility
    // ──────────────────────────────────────────────

    /** Sends a single-line message to the client. */
    private void send(String message) {
        out.println(message);
    }

    /** Logs a message with timestamp via the Server logger. */
    private void log(String message) {
        Server.log("[" + clientAddress + "] " + message);
    }

    /** Formats a byte count into a human-readable string. */
    private static String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
