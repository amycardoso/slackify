# Troubleshooting Guide for Trackify

## Railway Deployment Issues

### Error: "Could not resolve placeholder 'SLACK_CLIENT_ID' in value "${SLACK_CLIENT_ID}""

**Symptom:**
```
org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'slackConfig'
Caused by: java.lang.IllegalArgumentException: Could not resolve placeholder 'SLACK_CLIENT_ID' in value "${SLACK_CLIENT_ID}"
```

**Cause:**
Environment variables are NOT set in Railway's dashboard. Spring Boot cannot start without these required variables.

**Solution:**

1. **Go to Railway Dashboard:**
   - Navigate to https://railway.app
   - Select your Trackify project
   - Click the "Variables" tab

2. **Add ALL Required Variables:**

   Click "Raw Editor" and paste:
   ```bash
   SLACK_CLIENT_ID=your_actual_slack_client_id
   SLACK_CLIENT_SECRET=your_actual_slack_client_secret
   SLACK_SIGNING_SECRET=your_actual_slack_signing_secret
   SLACK_REDIRECT_URI=https://your-app.railway.app/oauth/slack/callback
   SLACK_BOT_TOKEN=
   SPOTIFY_CLIENT_ID=your_actual_spotify_client_id
   SPOTIFY_CLIENT_SECRET=your_actual_spotify_client_secret
   SPOTIFY_REDIRECT_URI=https://your-app.railway.app/oauth/spotify/callback
   SPRING_DATA_MONGODB_URI=your_mongodb_connection_string
   ENCRYPTION_SECRET_KEY=your_32_character_secret_key
   ```

3. **Click "Save"**

4. **Redeploy:**
   - Railway will automatically redeploy with the new variables
   - OR click "Redeploy" in the deployment tab

5. **Check Logs:**
   ```bash
   railway logs --follow
   ```

**Related Files:**
- See [RAILWAY_ENV_SETUP.md](RAILWAY_ENV_SETUP.md) for detailed instructions
- See [.env.example](.env.example) for local development reference

---

### Error: "Failed to configure a DataSource" or MongoDB Connection Issues

**Symptom:**
```
Failed to configure a DataSource: 'url' attribute is not specified
```
OR
```
MongoSocketOpenException: Exception opening socket
```

**Cause:**
MongoDB connection string is missing or invalid.

**Solution:**

1. **Verify MongoDB Connection String:**
   - For Railway MongoDB Plugin: `MONGODB_URL` variable should be auto-generated
   - For MongoDB Atlas: Get connection string from Atlas dashboard

2. **Set in Railway:**
   ```bash
   SPRING_DATA_MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/trackify?retryWrites=true&w=majority
   ```

3. **For MongoDB Atlas:**
   - Whitelist all IPs: `0.0.0.0/0` in Network Access
   - Verify database user has read/write permissions
   - Database name should be `trackify`

4. **Test Connection Locally First:**
   ```bash
   mongosh "mongodb+srv://username:password@cluster.mongodb.net/trackify"
   ```

---

### Error: "The Tomcat connector configured to listen on port X failed to start"

**Symptom:**
```
The Tomcat connector configured to listen on port 8080 failed to start. The port may already be in use
```

**Cause:**
Port conflict or incorrect PORT configuration.

**Solution:**

1. **Railway Auto-Sets PORT:**
   - Do NOT set `PORT` variable manually in Railway
   - Railway automatically assigns a port

2. **Remove PORT Variable:**
   - Go to Railway Variables tab
   - Delete `PORT` variable if you added it
   - Redeploy

3. **Verify application.properties:**
   - Should have: `server.port=${PORT:8080}`
   - This uses Railway's PORT or defaults to 8080 locally

---

### Error: "Invalid encryption key" or Encryption Issues

**Symptom:**
```
javax.crypto.BadPaddingException: Given final block not properly padded
```
OR
```
java.security.InvalidKeyException: Invalid AES key length
```

**Cause:**
`ENCRYPTION_SECRET_KEY` is missing or too short.

**Solution:**

1. **Generate Secure Key:**
   ```bash
   openssl rand -base64 32
   ```

2. **Add to Railway:**
   ```bash
   ENCRYPTION_SECRET_KEY=YourGeneratedBase64Key123456789012
   ```

3. **Requirements:**
   - Must be at least 32 characters
   - Should be random and secure
   - Same key must be used across deployments (or tokens can't be decrypted)

---

## Local Development Issues

### Error: "SLACK_CLIENT_ID not found" (Local)

**Cause:**
Environment variables not set in local environment.

**Solution:**

1. **Create `.env` file:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your actual values:**
   ```bash
   SLACK_CLIENT_ID=123456789.987654321
   SLACK_CLIENT_SECRET=abc123...
   # ... etc
   ```

3. **Source the file before running:**
   ```bash
   source .env
   ./gradlew bootRun
   ```

   OR use direnv:
   ```bash
   brew install direnv
   direnv allow .
   ./gradlew bootRun
   ```

---

### Error: MongoDB Connection Refused (Local)

**Symptom:**
```
com.mongodb.MongoSocketOpenException: Exception opening socket
```

**Cause:**
Local MongoDB is not running.

**Solution:**

1. **Start MongoDB:**
   ```bash
   # macOS with Homebrew
   brew services start mongodb-community

   # Linux
   sudo systemctl start mongod

   # Docker
   docker run -d -p 27017:27017 --name mongodb mongo:latest
   ```

2. **Verify MongoDB is Running:**
   ```bash
   mongosh
   # Should connect without errors
   ```

3. **Check Connection String:**
   - Local: `mongodb://localhost:27017/trackify`
   - Docker: `mongodb://host.docker.internal:27017/trackify`

---

## Slack Integration Issues

### Error: "users.profile:write does not exist"

**Cause:**
Scopes added to wrong section in Slack App configuration.

**Solution:**

1. **Go to Slack App Settings:**
   - https://api.slack.com/apps
   - Select your Trackify app

2. **Add to USER TOKEN SCOPES (NOT Bot Token Scopes):**
   - Navigate to "OAuth & Permissions"
   - Scroll to "User Token Scopes" section
   - Add:
     - `users.profile:write`
     - `users.profile:read`

3. **Reinstall App:**
   - Click "Reinstall App" button
   - Authorize in your workspace

**See:** [SLACK_SCOPES_FIX.md](SLACK_SCOPES_FIX.md) for detailed explanation

---

### Error: "OAuth redirect_uri mismatch"

**Symptom:**
```
redirect_uri_mismatch
The redirect URI provided does not match
```

**Cause:**
Redirect URI in Slack app doesn't match the one your application uses.

**Solution:**

1. **Update Slack App Redirect URIs:**
   - Go to https://api.slack.com/apps
   - Select your app
   - "OAuth & Permissions" → "Redirect URLs"
   - Add: `https://your-app.railway.app/oauth/slack/callback`

2. **Update Environment Variable:**
   ```bash
   SLACK_REDIRECT_URI=https://your-app.railway.app/oauth/slack/callback
   ```

3. **Ensure Exact Match:**
   - Must include `https://`
   - No trailing slashes
   - Correct domain (Railway or custom)

---

## Spotify Integration Issues

### Error: "Invalid redirect URI" (Spotify)

**Cause:**
Redirect URI in Spotify app doesn't match.

**Solution:**

1. **Update Spotify App Settings:**
   - Go to https://developer.spotify.com/dashboard
   - Select your app
   - Click "Edit Settings"
   - Add to Redirect URIs: `https://your-app.railway.app/oauth/spotify/callback`
   - Click "Save"

2. **Update Environment Variable:**
   ```bash
   SPOTIFY_REDIRECT_URI=https://your-app.railway.app/oauth/spotify/callback
   ```

---

### Error: "Insufficient client scope"

**Cause:**
Spotify scopes not requested during authorization.

**Solution:**

1. **Check SpotifyService.java:**
   - Should request: `user-read-currently-playing`, `user-read-playback-state`, `user-modify-playback-state`

2. **Re-authorize:**
   - Disconnect Spotify in dashboard
   - Reconnect to request proper scopes

---

## Build Issues

### Error: "No matching toolchains found for requested specification: {languageVersion=25}"

**Cause:**
Gradle version doesn't support Java 25.

**Solution:**

1. **Update Gradle Wrapper:**
   ```bash
   ./gradlew wrapper --gradle-version 9.1.0
   ```

2. **Verify Gradle Version:**
   ```bash
   ./gradlew --version
   # Should show: Gradle 9.1.0
   ```

3. **Rebuild:**
   ```bash
   ./gradlew clean build
   ```

**See:** [JAVA25_UPGRADE.md](JAVA25_UPGRADE.md) for details

---

### Error: "Could not resolve dependencies"

**Cause:**
Network issues or incorrect repository configuration.

**Solution:**

1. **Check Internet Connection**

2. **Clear Gradle Cache:**
   ```bash
   rm -rf ~/.gradle/caches
   ./gradlew clean build --refresh-dependencies
   ```

3. **Use Gradle Wrapper:**
   ```bash
   ./gradlew clean build
   # NOT: gradle clean build
   ```

---

## Runtime Issues

### Error: "Status update failed" in Logs

**Symptom:**
```
ERROR com.trackify.trackify.service.SlackService - Failed to update Slack status for user
```

**Cause:**
- Expired or invalid Slack token
- User revoked access
- Network issues

**Solution:**

1. **Check Slack Token:**
   - Verify token is still valid
   - User may need to re-authenticate

2. **Check Logs for Details:**
   ```bash
   railway logs --follow
   ```

3. **Test Slack API:**
   ```bash
   curl -X POST https://slack.com/api/users.profile.set \
     -H "Authorization: Bearer YOUR_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"profile":{"status_text":"Test"}}'
   ```

---

### Error: "Spotify token expired"

**Symptom:**
```
ERROR com.trackify.trackify.service.SpotifyService - Failed to refresh Spotify token
```

**Cause:**
Refresh token is invalid or expired.

**Solution:**

1. **User Must Re-authenticate:**
   - Disconnect Spotify in dashboard
   - Reconnect to get new tokens

2. **Check Spotify App Status:**
   - Ensure app is not in "Development Mode" with restrictions

---

## Helpful Commands

### Check Railway Logs
```bash
railway logs --follow
```

### Check Railway Variables
```bash
railway variables
```

### Test Local Build
```bash
./gradlew clean build
./gradlew bootRun
```

### Test Docker Build
```bash
docker build -t trackify .
docker run -p 8080:8080 --env-file .env trackify
```

### Check MongoDB Connection
```bash
mongosh "YOUR_MONGODB_URI"
```

### Test Health Endpoint
```bash
curl http://localhost:8080/health
```

---

## Getting Help

If none of these solutions work:

1. **Check Logs First:**
   - Railway: `railway logs --follow`
   - Local: Check console output

2. **Verify All Environment Variables:**
   - Railway: Check Variables tab
   - Local: Check `.env` file

3. **Review Documentation:**
   - [README.md](README.md) - Main documentation
   - [SETUP_GUIDE.md](SETUP_GUIDE.md) - Setup instructions
   - [RAILWAY_ENV_SETUP.md](RAILWAY_ENV_SETUP.md) - Environment variables
   - [RAILWAY_DEPLOYMENT.md](RAILWAY_DEPLOYMENT.md) - Deployment guide

4. **Common Checklist:**
   - [ ] All environment variables set in Railway
   - [ ] MongoDB connection string is valid
   - [ ] Slack app has correct scopes (User Token Scopes)
   - [ ] Spotify app has correct redirect URIs
   - [ ] Slack app has correct redirect URIs
   - [ ] ENCRYPTION_SECRET_KEY is at least 32 characters
   - [ ] No PORT variable set manually in Railway
   - [ ] Gradle 9.1.0 or higher for Java 25

---

**Last Updated:** October 26, 2025
