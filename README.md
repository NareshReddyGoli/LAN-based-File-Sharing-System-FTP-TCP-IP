# LAN File Sharing System

A distributed LAN-based secure classroom file sharing system built in Java.

## Architecture

```
 ┌───────────────────────┐         LAN (TCP:5050)         ┌────────────────────────┐
 │   FACULTY CABIN PC    │ ◄─────────────────────────────► │  CLASSROOM SMART BOARD │
 │  (Server Application) │   Hostname-based connection    │  (Client Application)  │
 │                       │   SHA-256 authenticated        │                        │
 │  Shares:              │   Buffered file transfer       │  Downloads to:         │
 │  C:\Class Room Share  │                                │  C:\TempClassFiles     │
 │       Folder          │                                │  (auto-deleted on exit)│
 └───────────────────────┘                                └────────────────────────┘
```

- **No central server** — each Faculty PC is an independent server
- **DHCP compatible** — uses hostnames, not static IPs
- **No internet dependency** — works entirely over LAN
- **Multi-threaded** — handles multiple classroom connections

## Project Structure

```
LANFileSharingSystem/
├── src/
│   ├── common/
│   │   ├── SecurityUtil.java       # SHA-256 hashing & authentication
│   │   └── Protocol.java          # Protocol constants & message types
│   ├── server/
│   │   ├── Server.java            # Main server (thread-pool, lifecycle)
│   │   └── ClientHandler.java     # Per-client handler (auth + transfer)
│   └── client/
│       └── Client.java            # GUI client (login, progress, cleanup)
├── build/                          # Compiled .class files (auto-generated)
├── build.bat                       # Compile all modules
├── run_server.bat                 # Launch server
├── run_client.bat                 # Launch client
├── CREDENTIALS.txt                # Default test credentials
├── SETUP_INSTRUCTIONS.md          # Full deployment guide
└── README.md                      # This file
```

## Quick Start

### 1. Build

```cmd
build.bat
```

### 2. Start Server (on Faculty PC)

```cmd
run_server.bat faculty1
```

### 3. Start Client (on Classroom PC)

```cmd
run_client.bat
```

Login with `faculty1` / `pass123`.

## Default Credentials

| Username  | Password | Mapped Hostname |
|-----------|----------|-----------------|
| faculty1  | pass123  | FACULTY1-PC     |
| faculty2  | pass456  | FACULTY2-PC     |

> **Note:** Passwords are hashed with SHA-256 before transmission.

## Features

| Feature                   | Details |
|---------------------------|---------|
| Authentication            | SHA-256 hashed passwords, constant-time comparison |
| File Transfer             | 8 KB buffered streams, handles files of any size |
| Progress Tracking         | Real-time progress bar in client GUI |
| Multi-threading           | Thread pool (10 concurrent clients) |
| Folder Restriction        | Server only exposes one designated folder |
| Secure Exit               | Deletes all temp files, closes socket, logs out |
| Auto-Build                | Run scripts compile automatically if needed |
| Error Handling            | Timeouts, retry-friendly login, structured error messages |

## Requirements

- **Java**: JDK 8 or higher
- **OS**: Windows 10+
- **Network**: LAN connectivity
- **Permissions**: Administrator (firewall only)

## Security

- SHA-256 password hashing (never plain text)
- Constant-time hash comparison (prevents timing attacks)
- Restricted folder access (no directory traversal)
- Socket timeout (30s) prevents resource exhaustion
- Automatic temp file cleanup on exit

## Port

- **TCP 5050** — configure firewall if needed:
  ```cmd
  netsh advfirewall firewall add rule name="LAN File Share" dir=in action=allow protocol=TCP localport=5050
  ```
