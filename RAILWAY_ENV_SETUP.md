# Railway Environment Variables Setup Guide

## Critical: You MUST Set These Variables in Railway

The application **will not start** without these environment variables configured in Railway's dashboard.

## Required Environment Variables

Railway Dashboard > Your Project > Variables tab

### 1. Slack Configuration (Required)

```bash
SLACK_CLIENT_ID=123456789.987654321
SLACK_CLIENT_SECRET=your_slack_client_secret_here
SLACK_SIGNING_SECRET=your_slack_signing_secret_here
SLACK_REDIRECT_URI=https://your-app.railway.app/oauth/slack/callback
SLACK_BOT_TOKEN=
```

**Get these from:** https://api.slack.com/apps

### 2. Spotify Configuration (Required)

```bash
SPOTIFY_CLIENT_ID=your_spotify_client_id_here
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret_here
SPOTIFY_REDIRECT_URI=https://your-app.railway.app/oauth/spotify/callback
```

**Get these from:** https://developer.spotify.com/dashboard

### 3. MongoDB Configuration (Required)

```bash
SPRING_DATA_MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/trackify?retryWrites=true&w=majority
```

**Options:**
- Use Railway's MongoDB plugin (recommended)
- Use MongoDB Atlas (free tier available)

### 4. Encryption Configuration (Required)

```bash
ENCRYPTION_SECRET_KEY=your_32_character_or_longer_secret_key_here
```

**Generate a secure key:**
```bash
openssl rand -base64 32
```

### 5. Server Port (Automatic - Railway Sets This)

Railway automatically sets the `PORT` environment variable. **Do NOT set this manually.**

```bash
PORT=8080  # Railway will set this automatically
```

## How to Add Variables in Railway

### Method 1: Raw Editor (Fastest)

1. Go to Railway Dashboard
2. Select your Trackify project
3. Click "Variables" tab
4. Click "Raw Editor"
5. Paste all variables:

```bash
SLACK_CLIENT_ID=your_value_here
SLACK_CLIENT_SECRET=your_value_here
SLACK_SIGNING_SECRET=your_value_here
SLACK_REDIRECT_URI=https://your-app.railway.app/oauth/slack/callback
SLACK_BOT_TOKEN=
SPOTIFY_CLIENT_ID=your_value_here
SPOTIFY_CLIENT_SECRET=your_value_here
SPOTIFY_REDIRECT_URI=https://your-app.railway.app/oauth/spotify/callback
SPRING_DATA_MONGODB_URI=your_mongodb_connection_string_here
ENCRYPTION_SECRET_KEY=your_generated_key_here
```

6. Click "Save"

### Method 2: One by One

1. Go to Railway Dashboard
2. Select your Trackify project
3. Click "Variables" tab
4. Click "New Variable"
5. Add each variable with its value
6. Click "Add" for each

## Verification Checklist

After adding variables, verify:

- [ ] All 9 required variables are set (see list above)
- [ ] No variables have placeholder values like "your_value_here"
- [ ] SLACK_REDIRECT_URI uses your actual Railway domain
- [ ] SPOTIFY_REDIRECT_URI uses your actual Railway domain
- [ ] SPRING_DATA_MONGODB_URI is a valid MongoDB connection string
- [ ] ENCRYPTION_SECRET_KEY is at least 32 characters
- [ ] Redeploy the application after setting variables

## Common Errors and Solutions

### Error: "Could not resolve placeholder 'SLACK_CLIENT_ID'"

**Cause:** Environment variables not set in Railway dashboard

**Fix:**
1. Go to Railway Dashboard > Variables
2. Verify all required variables are present
3. Click "Redeploy" to restart with new variables

### Error: "Failed to configure a DataSource"

**Cause:** SPRING_DATA_MONGODB_URI not set or invalid

**Fix:**
1. Check MongoDB connection string is valid
2. For MongoDB Atlas: Whitelist all IPs (0.0.0.0/0)
3. Verify username/password are correct
4. Ensure database name is "trackify"

### Error: "The Tomcat connector configured to listen on port X failed to start"

**Cause:** Port conflict or PORT variable incorrectly set

**Fix:**
1. Remove PORT from your Railway variables (Railway sets this automatically)
2. Redeploy

### Error: "Invalid encryption key"

**Cause:** ENCRYPTION_SECRET_KEY not set or too short

**Fix:**
1. Generate new key: `openssl rand -base64 32`
2. Add to Railway variables
3. Redeploy

## Deployment Flow

1. **Set all environment variables in Railway** ← START HERE
2. Push code to GitHub
3. Railway auto-deploys
4. Check logs for errors
5. If successful, get your Railway URL
6. Update Slack and Spotify redirect URIs with Railway URL
7. Update SLACK_REDIRECT_URI and SPOTIFY_REDIRECT_URI in Railway variables
8. Redeploy one more time
9. Test OAuth flows

## Quick Test

After deployment, check these endpoints:

```bash
# Health check
curl https://your-app.railway.app/health

# Home page (should load)
curl https://your-app.railway.app/

# Variables are loaded (check logs)
railway logs
```

## Environment Variable Reference

| Variable | Required | Auto-Set | Format | Example |
|----------|----------|----------|--------|---------|
| `SLACK_CLIENT_ID` | Yes | No | String | `123456789.987654321` |
| `SLACK_CLIENT_SECRET` | Yes | No | String | `abc123def456...` |
| `SLACK_SIGNING_SECRET` | Yes | No | String | `abc123def456...` |
| `SLACK_REDIRECT_URI` | Yes | No | URL | `https://app.railway.app/oauth/slack/callback` |
| `SLACK_BOT_TOKEN` | No | No | String | Leave empty or set later |
| `SPOTIFY_CLIENT_ID` | Yes | No | String | `abc123def456...` |
| `SPOTIFY_CLIENT_SECRET` | Yes | No | String | `abc123def456...` |
| `SPOTIFY_REDIRECT_URI` | Yes | No | URL | `https://app.railway.app/oauth/spotify/callback` |
| `SPRING_DATA_MONGODB_URI` | Yes | No | Connection String | `mongodb+srv://...` |
| `ENCRYPTION_SECRET_KEY` | Yes | No | Base64 String | `abc123...` (32+ chars) |
| `PORT` | No | Yes (Railway) | Number | `8080` |

## Support

If you still get environment variable errors:

1. Screenshot Railway's Variables tab
2. Check Railway logs: `railway logs --follow`
3. Verify application.properties is using correct variable names
4. Ensure no typos in variable names (case-sensitive!)

---

**Important:** Always set environment variables BEFORE the first deployment. Railway will fail to start the application without them.
