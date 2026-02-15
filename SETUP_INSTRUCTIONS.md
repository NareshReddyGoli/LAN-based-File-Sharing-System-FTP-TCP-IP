# LAN File Sharing System — Setup & Deployment Guide

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Java Installation](#1-java-installation)
3. [Build the Project](#2-build-the-project)
4. [Faculty PC Setup (Server)](#faculty-pc-setup-server)
5. [Classroom PC Setup (Client)](#classroom-pc-setup-client)
6. [Firewall Configuration](#firewall-configuration)
7. [Disable Sleep Mode](#disable-sleep-mode)
8. [Auto-Start on Windows Boot](#auto-start-on-windows-boot)
9. [Network / DHCP Notes](#network--dhcp-notes)
10. [Troubleshooting](#troubleshooting)

---

## System Requirements

| Requirement       | Details                                  |
|-------------------|------------------------------------------|
| Operating System  | Windows 10 or higher                     |
| Java              | JDK 8+ (JRE is enough to run, JDK to build) |
| Network           | LAN connectivity between PCs             |
| Permissions       | Administrator rights (for firewall)      |

---

## 1. Java Installation

1. Download JDK from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/).
2. Install with default settings.
3. Verify:

```cmd
java -version
javac -version
```

Both commands should output version information.

---

## 2. Build the Project

Copy the project folder to both Faculty PC and Classroom PC. Then compile:

```cmd
build.bat
```

This compiles all three modules (`common`, `server`, `client`) in dependency order.

---

## Faculty PC Setup (Server)

### Step 1: Create the Shared Folder

```cmd
mkdir "C:\Class Room Share Folder"
```

Place the files you want to share inside this folder.

### Step 2: Set the Hostname

The client connects to the server using hostnames, not IPs. Rename the PC:

**For Faculty 1:**
```cmd
wmic computersystem where name="%computername%" rename name="FACULTY1-PC"
```

**For Faculty 2:**
```cmd
wmic computersystem where name="%computername%" rename name="FACULTY2-PC"
```

**Restart the computer** after renaming.

### Step 3: Start the Server

```cmd
run_server.bat faculty1
```

or

```cmd
run_server.bat faculty2
```

The server will display a startup banner with IP, hostname, and port information.

---

## Classroom PC Setup (Client)

### Step 1: Copy the Project

Copy the entire project folder to the classroom Smart Board PC.

### Step 2: Build (if not already built)

```cmd
build.bat
```

### Step 3: Start the Client

```cmd
run_client.bat
```

### Step 4: Login

1. Enter the faculty username (e.g., `faculty1`).
2. Enter the password (e.g., `pass123`).
3. Click **Sign In**.
4. Files download automatically with a progress bar.

### Step 5: Exit

Click **Exit** → confirm → all downloaded files are deleted, socket is closed.

---

## Firewall Configuration

The server listens on **TCP port 5050**. You must allow this through the Windows Firewall.

### Option A: Command Line (Recommended)

Run as **Administrator**:

```cmd
netsh advfirewall firewall add rule name="LAN File Share" dir=in action=allow protocol=TCP localport=5050
```

### Option B: Windows GUI

1. Open **Windows Defender Firewall with Advanced Security**
2. Click **Inbound Rules** → **New Rule…**
3. Select **Port** → **TCP** → **5050**
4. Select **Allow the connection**
5. Apply to all profiles (Domain, Private, Public)
6. Name: `LAN File Share`

### Verify

```cmd
netsh advfirewall firewall show rule name="LAN File Share"
```

---

## Disable Sleep Mode

The server PC must stay awake during class hours.

1. Open **Control Panel** → **Power Options**
2. Click **Change plan settings** for your active plan
3. Set both options to **Never**:
   - Turn off the display → **Never**
   - Put the computer to sleep → **Never**
4. Click **Save changes**

Or via command line (Administrator):

```cmd
powercfg -change -standby-timeout-ac 0
powercfg -change -monitor-timeout-ac 0
```

---

## Auto-Start on Windows Boot

### Server (Faculty PC)

**Option A: Task Scheduler (Recommended)**

1. Open **Task Scheduler** → **Create Basic Task**
2. Name: `LAN File Sharing Server`
3. Trigger: **When the computer starts**
4. Action: **Start a program**
5. Settings:
   - Program: `javaw`
   - Arguments: `-cp "C:\path\to\project\build" server.Server faculty1`
   - Start in: `C:\path\to\project`
6. Uncheck **Start the task only if the computer is on AC power**

**Option B: Startup Folder**

1. Press `Win + R` → type `shell:startup` → Enter
2. Create a file `start_server.bat`:

```batch
@echo off
cd /d "C:\path\to\project"
java -cp "build" server.Server faculty1
```

3. Save the `.bat` file in the startup folder.

### Client (Classroom PC)

1. Create a desktop shortcut
2. Target: `java -cp "C:\path\to\project\build" client.Client`
3. Start in: `C:\path\to\project`
4. Double-click to launch

---

## Network / DHCP Notes

This system is designed for **DHCP environments** — no static IP needed.

### How It Works

- Faculty PCs are renamed to specific hostnames (FACULTY1-PC, FACULTY2-PC)
- The client resolves these hostnames via Windows NetBIOS / DNS
- This works automatically on most LANs

### Test Connectivity

From the classroom PC:

```cmd
ping FACULTY1-PC
ping FACULTY2-PC
```

### If Hostname Resolution Fails

Add manual entries to `C:\Windows\System32\drivers\etc\hosts`:

```
192.168.1.100  FACULTY1-PC
192.168.1.101  FACULTY2-PC
```

Replace with actual IP addresses (find with `ipconfig` on each Faculty PC).

---

## Troubleshooting

| Problem                  | Solution |
|--------------------------|----------|
| `Connection refused`     | Check server is running; verify firewall allows port 5050 |
| `Authentication failed`  | Verify username/password; ensure correct faculty server is running |
| `Unknown host`           | PC hostname doesn't match; check with `hostname` command |
| `Hostname not resolved`  | Use `ping FACULTY1-PC`; add hosts file entry if needed |
| `Build failed`           | Ensure JDK (not just JRE) is installed; check `javac -version` |
| `Files not downloading`  | Check shared folder has files; check network stability |
| `Port already in use`    | Another server is running; close it or check with `netstat -an | findstr 5050` |
| `Socket timeout`         | Network issue; check LAN cable/WiFi on both sides |

### Debug Tips

1. **Server console** — shows all connection attempts, auth results, and transfer progress
2. **Client console** — run from command prompt (not double-click) to see error output
3. **Network test**: `telnet FACULTY1-PC 5050` (enable Telnet feature first)
4. **Port check**: `netstat -an | findstr 5050`
