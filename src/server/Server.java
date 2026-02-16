package server;

import common.Protocol;
import common.SecurityUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main server application for the LAN File Sharing System.
 * 
 * Runs on a Faculty Cabin PC, sharing files from C:\Class Room Share Folder
 * to classroom Smart Board clients over LAN using TCP sockets.
 * 
 * Features:
 * - Multi-threaded client handling via a thread pool
 * - SHA-256 authenticated access
 * - Restricted to a single shared folder (no directory traversal)
 * - Graceful shutdown via JVM shutdown hook
 * - DHCP-compatible (uses hostnames, not static IPs)
 * 
 * Usage:
 * java server.Server [faculty1|faculty2]
 * If no argument is given, defaults to "faculty1".
 */
public class Server {

    // ──────────────────────────────────────────────
    // Configuration
    // ──────────────────────────────────────────────

    /** Maximum concurrent client threads */
    private static final int MAX_THREADS = 10;

    /** Timestamp formatter for log output */
    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ──────────────────────────────────────────────
    // Instance state
    // ──────────────────────────────────────────────

    private final String facultyUsername;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;
    private final AtomicInteger activeClients = new AtomicInteger(0);
    private final AtomicInteger totalConnections = new AtomicInteger(0);

    /**
     * Constructs a new Server for the given faculty.
     *
     * @param facultyUsername "faculty1" or "faculty2"
     */
    public Server(String facultyUsername) {
        this.facultyUsername = facultyUsername;
    }

    // ──────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────

    /**
     * Starts the server: validates the shared folder, binds to the port,
     * and enters the accept loop.
     */
    public void start() {
        try {
            // Step 1 — Ensure the shared folder is ready
            validateSharedFolder();

            // Step 2 — Create the thread pool
            threadPool = Executors.newFixedThreadPool(MAX_THREADS);

            // Step 3 — Bind the server socket
            serverSocket = new ServerSocket(Protocol.PORT);
            running = true;

            // Step 4 — Print startup banner
            printBanner();

            // Step 5 — Accept loop
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    int connNum = totalConnections.incrementAndGet();
                    activeClients.incrementAndGet();

                    log("Connection #" + connNum + " from "
                            + clientSocket.getInetAddress().getHostAddress()
                            + " (active clients: " + activeClients.get() + ")");

                    // Delegate to a handler thread
                    ClientHandler handler = new ClientHandler(
                            clientSocket,
                            Protocol.SHARED_FOLDER,
                            facultyUsername,
                            activeClients);
                    threadPool.submit(handler);

                } catch (IOException e) {
                    if (running) {
                        log("ERROR accepting connection: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            log("FATAL — Failed to start server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stop();
        }
    }

    /**
     * Stops the server gracefully: closes the socket and shuts down the thread
     * pool.
     */
    public void stop() {
        running = false;

        // Close the server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log("Error closing server socket: " + e.getMessage());
            }
        }

        // Shut down the thread pool
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow();
        }

        log("Server stopped. Total connections served: " + totalConnections.get());
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    /**
     * Validates (and optionally creates) the shared folder.
     * Throws IOException if the path exists but is not a writable directory.
     */
    private void validateSharedFolder() throws IOException {
        Path path = Paths.get(Protocol.SHARED_FOLDER);

        if (!Files.exists(path)) {
            log("Shared folder not found — creating: " + Protocol.SHARED_FOLDER);
            Files.createDirectories(path);
            log("Shared folder created.");
        }

        if (!Files.isDirectory(path)) {
            throw new IOException("Path is not a directory: " + Protocol.SHARED_FOLDER);
        }
        if (!Files.isReadable(path)) {
            throw new IOException("Folder is not readable: " + Protocol.SHARED_FOLDER);
        }

        // Count files in the folder
        long fileCount = Files.list(path).filter(Files::isRegularFile).count();
        log("Shared folder validated — " + fileCount + " file(s) available.");
    }

    /**
     * Prints a startup banner with connection details.
     */
    private void printBanner() throws IOException {
        InetAddress local = InetAddress.getLocalHost();
        String border = "═".repeat(50);

        System.out.println();
        System.out.println("╔" + border + "╗");
        System.out.println("║   LAN FILE SHARING SERVER — STARTED             ║");
        System.out.println("╠" + border + "╣");
        System.out.printf("║  Faculty    : %-35s║%n", facultyUsername);
        System.out.printf("║  Port       : %-35d║%n", Protocol.PORT);
        System.out.printf("║  IP Address : %-35s║%n", local.getHostAddress());
        System.out.printf("║  Hostname   : %-35s║%n", local.getHostName());
        System.out.printf("║  Shared Dir : %-35s║%n", Protocol.SHARED_FOLDER);
        System.out.printf("║  Max Clients: %-35d║%n", MAX_THREADS);
        System.out.println("╠" + border + "╣");
        System.out.println("║  Waiting for connections...                      ║");
        System.out.println("║  Press Ctrl+C to stop the server.                ║");
        System.out.println("╚" + border + "╝");
        System.out.println();
    }

    /**
     * Logs a timestamped message to stdout.
     */
    static void log(String message) {
        System.out.println("[" + LocalDateTime.now().format(LOG_FMT) + "] " + message);
    }

    // ──────────────────────────────────────────────
    // Entry Point
    // ──────────────────────────────────────────────

    /**
     * Main entry point.
     * If run with a command-line arg, uses it as faculty username.
     * If double-clicked (no args), shows a GUI chooser dialog.
     */
    public static void main(String[] args) {
        String username = null;

        // ── Priority 1: Auto-detect hostname → match to faculty ──
        try {
            String detectedHostname = InetAddress.getLocalHost().getHostName();
            String autoMatch = Protocol.getFacultyForHostname(detectedHostname);

            if (autoMatch != null) {
                username = autoMatch;
                log("Hostname '" + detectedHostname + "' matched → auto-starting as '" + username + "'");
            } else {
                log("Hostname '" + detectedHostname + "' not found in faculty mappings.");
            }
        } catch (Exception e) {
            log("Could not detect hostname: " + e.getMessage());
        }

        // ── Priority 2: Command-line argument ──
        if (username == null && args.length > 0) {
            username = args[0].trim();
        }

        // ── Priority 3: GUI chooser (fallback) ──
        if (username == null) {
            try {
                javax.swing.UIManager.setLookAndFeel(
                        javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            String[] options = SecurityUtil.getAllUsernames()
                    .toArray(new String[0]);

            String choice = (String) javax.swing.JOptionPane.showInputDialog(
                    null,
                    "No hostname match found.\nSelect which faculty account to start the server as:",
                    "LAN File Sharing — Server Setup",
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == null) {
                System.out.println("Server startup cancelled.");
                System.exit(0);
            }
            username = choice;
        }

        // Validate
        if (!SecurityUtil.isValidUsername(username)) {
            System.err.println("ERROR: Unknown faculty username '" + username + "'");
            System.err.println("Registered faculty: " + SecurityUtil.getAllUsernames());
            System.exit(1);
        }

        Server server = new Server(username);

        // Register graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log("Shutdown signal received...");
            server.stop();
        }));

        // Start serving
        server.start();
    }
}
