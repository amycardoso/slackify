# Slackify Slash Commands Reference

Quick reference for all available Slackify commands in Slack.

## Command Syntax

All commands follow the pattern:
```
/slackify <subcommand>
```

## Available Commands

### 🎵 `/slackify play`
Resume Spotify playback.

**Example:**
```
/slackify play
```

**Response:**
```
▶️ Playback resumed!
```

**Requirements:**
- Spotify account connected
- Spotify client must be active (desktop/mobile/web)

---

### ⏸️ `/slackify pause`
Pause Spotify playback.

**Example:**
```
/slackify pause
```

**Response:**
```
⏸️ Playback paused!
```

**Requirements:**
- Spotify account connected
- Music currently playing

---

### 📊 `/slackify status`
Display your current sync status and settings.

**Example:**
```
/slackify status
```

**Response:**
```
🎵 Your Slackify Status

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

### 🔄 `/slackify sync`
Manually trigger an immediate music sync (bypasses the 10-second polling interval).

**Example:**
```
/slackify sync
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

### ✅ `/slackify enable`
Enable automatic music status synchronization.

**Example:**
```
/slackify enable
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

### 🚫 `/slackify disable`
Disable automatic music status synchronization.

**Example:**
```
/slackify disable
```

**Response:**
```
🚫 Music sync disabled!
```

**What it does:**
- Stops automatic polling
- Your Slack status won't update automatically
- Previous status remains until manually changed
- You can still use `/slackify play` and `/slackify pause`

---

### ❓ `/slackify help`
Display help information with all available commands.

**Example:**
```
/slackify help
```

**Response:**
```
🎵 Slackify Commands

/slackify play - Resume Spotify playback
/slackify pause - Pause Spotify playback
/slackify status - Show current sync status
/slackify sync - Manually trigger music sync
/slackify enable - Enable automatic music sync
/slackify disable - Disable automatic music sync
/slackify help - Show this help message

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
5. Type /slackify status to verify setup
```

### Daily Usage
```
# Enable sync when starting work
/slackify enable

# Your status updates automatically as you listen to music

# Disable sync when in meetings
/slackify disable
```

### Controlling Playback from Slack
```
# Pause music when someone calls
/slackify pause

# Resume after the call
/slackify play
```

### Troubleshooting
```
# Check if everything is connected
/slackify status

# Force an immediate sync
/slackify sync

# If status isn't updating, disable and re-enable
/slackify disable
/slackify enable
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
Use `/slackify status` to see what's currently playing without switching to Spotify

### 💡 Privacy Mode
Use `/slackify disable` when you don't want to share what you're listening to

### 💡 Remote Control
Control your music from Slack without opening Spotify - great for when you're deep in work

### 💡 Team Visibility
Your team can see what you're jamming to - great for discovering new music!

### 💡 Meeting Mode
Create a Slack workflow that automatically runs `/slackify disable` when you join a meeting

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
1. Run `/slackify status` to diagnose
2. Check the application logs
3. Contact your Slackify administrator
4. Open an issue on GitHub

---

Made with ❤️ for music lovers
