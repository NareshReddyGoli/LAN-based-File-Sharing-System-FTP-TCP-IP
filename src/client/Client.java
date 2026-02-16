package client;

import common.Protocol;
import common.SecurityUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI client application for the LAN File Sharing System.
 *
 * Features:
 * - Clean, modern login interface with gradient background
 * - Real-time progress bar during file downloads
 * - File manager with sizes and open-folder button
 * - Secure exit with temp-file cleanup
 */
public class Client {

    // ══════════════════════════════════════════════
    // COLOR PALETTE — Clean Modern Theme
    // ══════════════════════════════════════════════

    // Gradient background
    private static final Color GRAD_TOP = new Color(25, 25, 60); // Deep navy
    private static final Color GRAD_BOTTOM = new Color(45, 55, 110); // Rich indigo

    // Card & surfaces
    private static final Color CARD_BG = new Color(255, 255, 255, 240); // White card
    private static final Color CARD_BORDER = new Color(220, 225, 240); // Soft border

    // Text colors
    private static final Color TEXT_DARK = new Color(30, 30, 50); // Headings
    private static final Color TEXT_MEDIUM = new Color(100, 105, 125); // Body text
    private static final Color TEXT_LIGHT = new Color(150, 155, 170); // Hints

    // Input fields
    private static final Color INPUT_BG = new Color(245, 247, 252); // Light grey-blue
    private static final Color INPUT_BORDER = new Color(200, 205, 220); // Neutral border
    private static final Color INPUT_FOCUS = new Color(80, 120, 230); // Focus ring

    // Primary button
    private static final Color BTN_PRIMARY = new Color(65, 105, 225); // Royal Blue
    private static final Color BTN_HOVER = new Color(50, 90, 200); // Darker on hover
    private static final Color BTN_PRESSED = new Color(40, 75, 180); // Pressed state

    // Status colors
    private static final Color CLR_SUCCESS = new Color(40, 180, 100); // Green
    private static final Color CLR_ERROR = new Color(220, 60, 60); // Red
    private static final Color CLR_WARNING = new Color(240, 170, 30); // Amber

    // File manager (dark theme)
    private static final Color FM_BG = new Color(22, 22, 35); // Deep dark
    private static final Color FM_CARD = new Color(35, 35, 55); // Panel bg
    private static final Color FM_TEXT = new Color(220, 225, 240); // Light text
    private static final Color FM_ACCENT = new Color(100, 140, 255); // Blue accent

    // ══════════════════════════════════════════════
    // STATE
    // ══════════════════════════════════════════════

    private JFrame loginFrame;
    private JFrame fileFrame;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final List<String> downloadedFiles = new ArrayList<>();

    // ══════════════════════════════════════════════
    // ENTRY POINT
    // ══════════════════════════════════════════════

    public static void main(String[] args) {
        // Enable anti-aliased text rendering globally
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        new Client().start();
    }

    public void start() {
        SwingUtilities.invokeLater(this::createLoginGUI);
    }

    // ══════════════════════════════════════════════
    // LOGIN SCREEN
    // ══════════════════════════════════════════════

    private void createLoginGUI() {
        loginFrame = new JFrame("LAN File Sharing");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(500, 560);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setResizable(false);

        // ── Gradient background panel ──
        JPanel background = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, GRAD_TOP, 0, getHeight(), GRAD_BOTTOM);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };

        // ── White card ──
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(CARD_BORDER, 1),
                new EmptyBorder(40, 45, 35, 45)));
        card.setPreferredSize(new Dimension(400, 440));
        card.setOpaque(true);

        // ── Icon / Logo area ──
        JLabel iconLabel = new JLabel("\uD83D\uDCC1", JLabel.CENTER); // 📁 emoji
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(8));

        // Title
        JLabel title = new JLabel("Classroom File Sharing");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(4));

        // Subtitle
        JLabel subtitle = new JLabel("Sign in with your faculty credentials");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_LIGHT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(30));

        // ── Username field ──
        JLabel userLabel = makeFieldLabel("USERNAME");
        card.add(userLabel);
        card.add(Box.createVerticalStrut(6));

        JTextField usernameField = makeTextField("Enter your username");
        card.add(usernameField);
        card.add(Box.createVerticalStrut(18));

        // ── Password field ──
        JLabel passLabel = makeFieldLabel("PASSWORD");
        card.add(passLabel);
        card.add(Box.createVerticalStrut(6));

        JPasswordField passwordField = makePasswordField("Enter your password");
        card.add(passwordField);
        card.add(Box.createVerticalStrut(28));

        // ── Sign In button ──
        JButton loginBtn = makePrimaryButton("Sign In");
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(18));

        // ── Status message ──
        JLabel statusLabel = new JLabel(" ", JLabel.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(CLR_ERROR);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(statusLabel);

        background.add(card);
        loginFrame.setContentPane(background);

        // ── Login action ──
        Runnable loginAction = () -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setForeground(CLR_WARNING);
                statusLabel.setText("Please fill in both fields.");
                return;
            }

            statusLabel.setForeground(TEXT_MEDIUM);
            statusLabel.setText("Connecting to server...");
            loginBtn.setEnabled(false);
            loginBtn.setText("Connecting...");

            new Thread(() -> {
                boolean ok = login(username, password);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        loginFrame.dispose();
                        createFileGUI();
                    } else {
                        statusLabel.setForeground(CLR_ERROR);
                        statusLabel.setText("Login failed — check credentials or server.");
                        loginBtn.setEnabled(true);
                        loginBtn.setText("Sign In");
                    }
                });
            }).start();
        };

        loginBtn.addActionListener(e -> loginAction.run());
        passwordField.addActionListener(e -> loginAction.run());
        usernameField.addActionListener(e -> passwordField.requestFocus());

        loginFrame.setVisible(true);
        usernameField.requestFocusInWindow();
    }

    // ══════════════════════════════════════════════
    // FILE MANAGER SCREEN
    // ══════════════════════════════════════════════

    private void createFileGUI() {
        fileFrame = new JFrame("LAN File Sharing — Downloads");
        fileFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        fileFrame.setSize(700, 520);
        fileFrame.setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(0, 12));
        main.setBackground(FM_BG);
        main.setBorder(new EmptyBorder(18, 18, 18, 18));

        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel headerTitle = new JLabel("Downloaded Files");
        headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        headerTitle.setForeground(FM_TEXT);
        header.add(headerTitle, BorderLayout.WEST);

        JLabel headerSub = new JLabel("Files are saved to C:\\TempClassFiles");
        headerSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        headerSub.setForeground(new Color(120, 125, 150));
        header.add(headerSub, BorderLayout.EAST);

        main.add(header, BorderLayout.NORTH);

        // ── File list ──
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("  Waiting for downloads...");
        JList<String> fileList = new JList<>(listModel);
        fileList.setBackground(FM_CARD);
        fileList.setForeground(FM_TEXT);
        fileList.setSelectionBackground(FM_ACCENT);
        fileList.setSelectionForeground(Color.WHITE);
        fileList.setFont(new Font("Consolas", Font.PLAIN, 13));
        fileList.setFixedCellHeight(28);
        fileList.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane scroll = new JScrollPane(fileList);
        scroll.setBorder(new LineBorder(new Color(50, 50, 75), 1));
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        main.add(scroll, BorderLayout.CENTER);

        // ── Bottom section ──
        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);

        // Progress
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Preparing...");
        progressBar.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        progressBar.setForeground(FM_ACCENT);
        progressBar.setBackground(FM_CARD);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 22));

        JLabel statusLabel = new JLabel("Connecting to server...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(150, 155, 175));

        JPanel progressPanel = new JPanel(new BorderLayout(0, 5));
        progressPanel.setOpaque(false);
        progressPanel.add(statusLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        bottom.add(progressPanel, BorderLayout.NORTH);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        JButton refreshBtn = makeFMButton("Refresh", FM_ACCENT);
        JButton openBtn = makeFMButton("Open Folder", new Color(80, 170, 120));
        JButton exitBtn = makeFMButton("Exit", new Color(200, 70, 70));

        refreshBtn.addActionListener(e -> refreshFileList(listModel));
        openBtn.addActionListener(e -> openTempFolder());
        exitBtn.addActionListener(e -> exitApplication());

        btnPanel.add(refreshBtn);
        btnPanel.add(openBtn);
        btnPanel.add(exitBtn);
        bottom.add(btnPanel, BorderLayout.SOUTH);

        main.add(bottom, BorderLayout.SOUTH);
        fileFrame.setContentPane(main);

        fileFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        fileFrame.setVisible(true);

        // Start downloads
        new Thread(() -> downloadFiles(listModel, progressBar, statusLabel)).start();
    }

    // ══════════════════════════════════════════════
    // NETWORK — Login
    // ══════════════════════════════════════════════

    private boolean login(String username, String password) {
        try {
            String hostname = Protocol.getHostnameForFaculty(username);
            if (hostname == null) {
                System.err.println("Unknown faculty: " + username);
                return false;
            }

            System.out.println("Connecting to " + hostname + "...");
            socket = new Socket(hostname, Protocol.PORT);
            socket.setSoTimeout(Protocol.SOCKET_TIMEOUT_MS);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send username + SHA-256 hash (never plain text)
            out.println(username);
            out.println(SecurityUtil.hashPassword(password));

            String response = in.readLine();
            return Protocol.AUTH_SUCCESS.equals(response);

        } catch (IOException e) {
            System.err.println("Login error: " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════
    // NETWORK — Downloads
    // ══════════════════════════════════════════════

    private void downloadFiles(DefaultListModel<String> listModel,
            JProgressBar progressBar, JLabel statusLabel) {
        try {
            createTempFolder();
            String response = in.readLine();

            if (Protocol.NO_FILES.equals(response)) {
                updateUI(() -> {
                    statusLabel.setText("No files available on server.");
                    progressBar.setValue(100);
                    progressBar.setString("No files");
                    listModel.clear();
                    listModel.addElement("  (no files on server)");
                });
                return;
            }

            if (response != null && response.startsWith(Protocol.ERROR_PREFIX)) {
                String err = response.substring(Protocol.ERROR_PREFIX.length());
                updateUI(() -> {
                    statusLabel.setText("Server error: " + err);
                    progressBar.setForeground(CLR_ERROR);
                });
                return;
            }

            if (response == null || !response.startsWith(Protocol.FILE_COUNT_PREFIX)) {
                updateUI(() -> statusLabel.setText("Unexpected server response."));
                return;
            }

            int fileCount = Integer.parseInt(response.substring(Protocol.FILE_COUNT_PREFIX.length()));
            updateUI(() -> {
                listModel.clear();
                statusLabel.setText("Downloading " + fileCount + " file(s)...");
            });

            int successCount = 0;

            for (int i = 0; i < fileCount; i++) {
                String fileInfo = in.readLine();
                if (fileInfo == null || !fileInfo.startsWith(Protocol.FILE_INFO_PREFIX))
                    break;

                String payload = fileInfo.substring(Protocol.FILE_INFO_PREFIX.length());
                int lastColon = payload.lastIndexOf(Protocol.DELIMITER);
                if (lastColon <= 0)
                    break;

                String fileName = payload.substring(0, lastColon);
                long fileSize = Long.parseLong(payload.substring(lastColon + 1));

                final int fileNum = i + 1;
                updateUI(() -> statusLabel.setText("Downloading (" + fileNum + "/" + fileCount + "): " + fileName));

                out.println(Protocol.READY);

                boolean ok = downloadFile(fileName, fileSize, progressBar, fileNum, fileCount);
                if (ok) {
                    downloadedFiles.add(fileName);
                    final String entry = String.format("  %-42s %s", fileName, formatSize(fileSize));
                    updateUI(() -> listModel.addElement(entry));
                    out.println(Protocol.FILE_RECEIVED);
                    successCount++;
                } else {
                    out.println(Protocol.FILE_ERROR);
                    break;
                }
            }

            in.readLine(); // consume TRANSFER_COMPLETE

            final int sc = successCount;
            updateUI(() -> {
                statusLabel.setText("Done — " + sc + " file(s) downloaded successfully.");
                statusLabel.setForeground(CLR_SUCCESS);
                progressBar.setValue(100);
                progressBar.setString("Complete ✓");
                progressBar.setForeground(CLR_SUCCESS);
            });

        } catch (Exception e) {
            System.err.println("Download error: " + e.getMessage());
            updateUI(() -> {
                statusLabel.setText("Error: " + e.getMessage());
                statusLabel.setForeground(CLR_ERROR);
            });
        }
    }

    private boolean downloadFile(String fileName, long fileSize,
            JProgressBar progressBar,
            int currentFile, int totalFiles) {
        Path filePath = Paths.get(Protocol.TEMP_FOLDER, fileName);

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
                BufferedOutputStream bos = new BufferedOutputStream(fos, Protocol.BUFFER_SIZE)) {

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            byte[] buffer = new byte[Protocol.BUFFER_SIZE];
            long totalRead = 0;

            while (totalRead < fileSize) {
                int toRead = (int) Math.min(buffer.length, fileSize - totalRead);
                int bytesRead = dis.read(buffer, 0, toRead);
                if (bytesRead == -1)
                    break;

                bos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                final int fileProgress = (int) ((totalRead * 100) / fileSize);
                final int overall = (int) (((currentFile - 1) * 100L + fileProgress) / totalFiles);
                final long tr = totalRead;

                updateUI(() -> {
                    progressBar.setValue(overall);
                    progressBar.setString(formatSize(tr) + " / " + formatSize(fileSize) + "  (" + overall + "%)");
                });
            }
            bos.flush();
            return totalRead == fileSize;

        } catch (IOException e) {
            System.err.println("Error downloading " + fileName + ": " + e.getMessage());
            return false;
        }
    }

    // ══════════════════════════════════════════════
    // EXIT & CLEANUP
    // ══════════════════════════════════════════════

    private void exitApplication() {
        int choice = JOptionPane.showConfirmDialog(fileFrame,
                "Log out and delete all downloaded files?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                cleanup();
                SwingUtilities.invokeLater(() -> {
                    if (fileFrame != null)
                        fileFrame.dispose();
                    downloadedFiles.clear();
                    createLoginGUI(); // Go back to login instead of closing
                });
            }).start();
        }
    }

    private void cleanup() {
        try {
            if (out != null)
                out.close();
        } catch (Exception ignored) {
        }
        try {
            if (in != null)
                in.close();
        } catch (Exception ignored) {
        }
        try {
            if (socket != null)
                socket.close();
        } catch (Exception ignored) {
        }
        deleteTempFiles();
        System.out.println("Cleanup complete — all temp files removed.");
    }

    private void deleteTempFiles() {
        Path tempPath = Paths.get(Protocol.TEMP_FOLDER);
        if (!Files.exists(tempPath))
            return;
        try {
            Files.list(tempPath).filter(Files::isRegularFile).forEach(path -> {
                try {
                    Files.delete(path);
                    System.out.println("Deleted: " + path.getFileName());
                } catch (IOException e) {
                    System.err.println("Could not delete " + path);
                }
            });
        } catch (IOException e) {
            System.err.println("Error scanning temp folder: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════
    // UTILITY
    // ══════════════════════════════════════════════

    private void createTempFolder() throws IOException {
        Path p = Paths.get(Protocol.TEMP_FOLDER);
        if (!Files.exists(p))
            Files.createDirectories(p);
    }

    private void refreshFileList(DefaultListModel<String> model) {
        model.clear();
        Path tempPath = Paths.get(Protocol.TEMP_FOLDER);
        if (Files.exists(tempPath)) {
            try {
                Files.list(tempPath).filter(Files::isRegularFile).sorted().forEach(path -> {
                    try {
                        long size = Files.size(path);
                        model.addElement(String.format("  %-42s %s", path.getFileName(), formatSize(size)));
                    } catch (IOException ignored) {
                        model.addElement("  " + path.getFileName());
                    }
                });
            } catch (IOException e) {
                model.addElement("  Error reading folder.");
            }
        }
        if (model.isEmpty())
            model.addElement("  (no files downloaded)");
    }

    private void openTempFolder() {
        try {
            Path p = Paths.get(Protocol.TEMP_FOLDER);
            if (Files.exists(p))
                Desktop.getDesktop().open(p.toFile());
            else
                JOptionPane.showMessageDialog(fileFrame, "Folder does not exist yet.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(fileFrame, "Cannot open folder: " + e.getMessage());
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private void updateUI(Runnable task) {
        SwingUtilities.invokeLater(task);
    }

    // ══════════════════════════════════════════════
    // UI COMPONENT FACTORIES
    // ══════════════════════════════════════════════

    /** Creates a small uppercase field label */
    private JLabel makeFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(TEXT_MEDIUM);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Creates a styled text field with placeholder */
    private JTextField makeTextField(String placeholder) {
        JTextField tf = new JTextField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(TEXT_LIGHT);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    Insets ins = getInsets();
                    g2.drawString(placeholder, ins.left + 2, getHeight() / 2 + 5);
                    g2.dispose();
                }
            }
        };
        styleInput(tf);
        return tf;
    }

    /** Creates a styled password field with placeholder */
    private JPasswordField makePasswordField(String placeholder) {
        JPasswordField pf = new JPasswordField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getPassword().length == 0 && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(TEXT_LIGHT);
                    g2.setFont(getFont().deriveFont(Font.ITALIC, 13f));
                    Insets ins = getInsets();
                    g2.drawString(placeholder, ins.left + 2, getHeight() / 2 + 5);
                    g2.dispose();
                }
            }
        };
        styleInput(pf);
        return pf;
    }

    /** Shared styling for text inputs */
    private void styleInput(JTextField tf) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        tf.setPreferredSize(new Dimension(300, 42));
        tf.setBackground(INPUT_BG);
        tf.setForeground(TEXT_DARK);
        tf.setCaretColor(TEXT_DARK);
        tf.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(INPUT_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)));

        // Focus highlight
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(INPUT_FOCUS, 2),
                        new EmptyBorder(7, 11, 7, 11)));
                tf.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(INPUT_BORDER, 1),
                        new EmptyBorder(8, 12, 8, 12)));
                tf.repaint();
            }
        });
    }

    /** Creates the primary action button */
    private JButton makePrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setBackground(BTN_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 44));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled())
                    btn.setBackground(BTN_HOVER);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(BTN_PRIMARY);
            }

            public void mousePressed(MouseEvent e) {
                if (btn.isEnabled())
                    btn.setBackground(BTN_PRESSED);
            }

            public void mouseReleased(MouseEvent e) {
                if (btn.isEnabled())
                    btn.setBackground(BTN_HOVER);
            }
        });
        return btn;
    }

    /** Creates a file-manager-style button */
    private JButton makeFMButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(115, 34));

        Color hoverColor = color.darker();
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverColor);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        return btn;
    }
}
