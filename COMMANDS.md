# Trackify Slash Commands Reference

Quick reference for all available Trackify commands in Slack.

## Command Syntax

All commands follow the pattern:
```
/trackify <subcommand>
```

## Available Commands

### 🎵 `/trackify play`
Resume Spotify playback.

**Example:**
```
/trackify play
```

**Response:**
```
▶️ Playback resumed!
```

**Requirements:**
- Spotify account connected
- Spotify client must be active (desktop/mobile/web)

---

### ⏸️ `/trackify pause`
Pause Spotify playback.

**Example:**
```
/trackify pause
```

**Response:**
```
⏸️ Playback paused!
```

**Requirements:**
- Spotify account connected
- Music currently playing

---

### 📊 `/trackify status`
Display your current sync status and settings.

**Example:**
```
/trackify status
```

**Response:**
```
🎵 Your Trackify Status

Sync Status: ✅ Enabled
Spotify: ✅ Connected
Now Playing: Shape of You - Ed Sheeran

Settings:
• Emoji: 🎵
• Show Artist: Yes
• Show Title: Yes
• Notifications: Disabled
```

---

### 🔄 `/trackify sync`
Manually trigger an immediate music sync (bypasses the 10-second polling interval).

**Example:**
```
/trackify sync
```

**Response:**
```
🔄 Manual sync triggered!
```

**Use cases:**
- Force immediate status update
- Test if sync is working
- Update status after changing songs

---

### ✅ `/trackify enable`
Enable automatic music status synchronization.

**Example:**
```
/trackify enable
```

**Response:**
```
✅ Music sync enabled!
```

**What it does:**
- Turns on automatic polling
- Your Slack status will update when songs change
- Syncs every 10 seconds

---

### 🚫 `/trackify disable`
Disable automatic music status synchronization.

**Example:**
```
/trackify disable
```

**Response:**
```
🚫 Music sync disabled!
```

**What it does:**
- Stops automatic polling
- Your Slack status won't update automatically
- Previous status remains until manually changed
- You can still use `/trackify play` and `/trackify pause`

---

### ❓ `/trackify help`
Display help information with all available commands.

**Example:**
```
/trackify help
```

**Response:**
```
🎵 Trackify Commands

/trackify play - Resume Spotify playback
/trackify pause - Pause Spotify playback
/trackify status - Show current sync status
/trackify sync - Manually trigger music sync
/trackify enable - Enable automatic music sync
/trackify disable - Disable automatic music sync
/trackify help - Show this help message

🔗 To get started, connect your accounts at: /oauth/slack
```

---

## Common Use Cases

### First Time Setup
```
1. Visit http://localhost:8080 (or your deployed URL)
2. Click "Get Started - Connect with Slack"
3. Authorize Slack
4. Authorize Spotify
5. Type /trackify status to verify setup
```

### Daily Usage
```
# Enable sync when starting work
/trackify enable

# Your status updates automatically as you listen to music

# Disable sync when in meetings
/trackify disable
```

### Controlling Playback from Slack
```
# Pause music when someone calls
/trackify pause

# Resume after the call
/trackify play
```

### Troubleshooting
```
# Check if everything is connected
/trackify status

# Force an immediate sync
/trackify sync

# If status isn't updating, disable and re-enable
/trackify disable
/trackify enable
```

## Error Messages

### ❌ "You need to connect your Spotify account first"
**Cause:** Spotify not authorized
**Solution:** Visit the app URL and complete Spotify OAuth

### ❌ "Your Spotify account is not connected"
**Cause:** Spotify token expired or revoked
**Solution:** Re-authorize Spotify through the app

### ❌ "Failed to resume playback"
**Cause:**
- No active Spotify device
- Spotify Premium required
- Network issues

**Solution:**
- Open Spotify on any device
- Ensure Premium subscription
- Check internet connection

### ❌ "User settings not found"
**Cause:** Database issue or first-time setup incomplete
**Solution:** Contact administrator or re-authorize

## Tips & Tricks

### 💡 Quick Status Check
Use `/trackify status` to see what's currently playing without switching to Spotify

### 💡 Privacy Mode
Use `/trackify disable` when you don't want to share what you're listening to

### 💡 Remote Control
Control your music from Slack without opening Spotify - great for when you're deep in work

### 💡 Team Visibility
Your team can see what you're jamming to - great for discovering new music!

### 💡 Meeting Mode
Create a Slack workflow that automatically runs `/trackify disable` when you join a meeting

## Customization

Want to customize your music status? Check the user settings in your database or contact the admin to add a settings UI.

Current customizable options:
- Status emoji (default: 🎵)
- Show/hide artist name
- Show/hide song title
- Status template format
- Enable/disable notifications

## Support

Having issues?
1. Run `/trackify status` to diagnose
2. Check the application logs
3. Contact your Trackify administrator
4. Open an issue on GitHub

---

Made with ❤️ for music lovers
