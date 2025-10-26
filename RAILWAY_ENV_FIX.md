# Railway Environment Variables Fix

## Problem

You're getting this error on Railway:
```
Could not resolve placeholder 'SLACK_CLIENT_ID' in value "${SLACK_CLIENT_ID}" <-- "${slack.client-id}"
```

## Root Cause

**The environment variables are NOT set in Railway's dashboard.**

When Spring Boot starts, it tries to load `application.properties` which references environment variables like `${SLACK_CLIENT_ID}`. If these variables aren't set in Railway, Spring Boot cannot start.

## Solution

### Step 1: Set Environment Variables in Railway

1. **Go to Railway Dashboard:**
   - https://railway.app
   - Select your Trackify project
   - Click **"Variables"** tab

2. **Click "Raw Editor"** (fastest method)

3. **Paste ALL these variables** (replace with your actual values):

```bash
SLACK_CLIENT_ID=your_slack_client_id_from_slack_api
SLACK_CLIENT_SECRET=your_slack_client_secret_from_slack_api
SLACK_SIGNING_SECRET=your_slack_signing_secret_from_slack_api
SLACK_REDIRECT_URI=https://your-app-name.railway.app/oauth/slack/callback
SLACK_BOT_TOKEN=

SPOTIFY_CLIENT_ID=your_spotify_client_id_from_spotify_dashboard
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret_from_spotify_dashboard
SPOTIFY_REDIRECT_URI=https://your-app-name.railway.app/oauth/spotify/callback

SPRING_DATA_MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/trackify?retryWrites=true&w=majority

ENCRYPTION_SECRET_KEY=your_32_character_or_longer_random_key
```

4. **Click "Save"**

5. Railway will automatically redeploy with the new variables

### Step 2: Get Your Actual Values

#### Slack Values
1. Go to https://api.slack.com/apps
2. Select your Trackify app
3. Get:
   - **Client ID** from "Basic Information" → "App Credentials"
   - **Client Secret** from "Basic Information" → "App Credentials"
   - **Signing Secret** from "Basic Information" → "App Credentials"

#### Spotify Values
1. Go to https://developer.spotify.com/dashboard
2. Select your Trackify app
3. Click "Settings"
4. Get:
   - **Client ID** (shown on main page)
   - **Client Secret** (click "View client secret")

#### MongoDB Connection String
- **Option A:** Use Railway's MongoDB plugin (recommended)
  1. In Railway, click "New" → "Database" → "Add MongoDB"
  2. Copy the `MONGO_URL` variable
  3. Use that for `SPRING_DATA_MONGODB_URI`

- **Option B:** Use MongoDB Atlas (free tier)
  1. Create cluster at https://cloud.mongodb.com
  2. Get connection string
  3. Format: `mongodb+srv://username:password@cluster.mongodb.net/trackify?retryWrites=true&w=majority`

#### Encryption Key
Generate a secure random key:
```bash
openssl rand -base64 32
```

Copy the output and use it for `ENCRYPTION_SECRET_KEY`.

### Step 3: Update Redirect URIs

After Railway generates your domain (e.g., `trackify.railway.app`):

1. **Update Slack App:**
   - Go to https://api.slack.com/apps
   - Select your app → "OAuth & Permissions"
   - Add Redirect URL: `https://trackify.railway.app/oauth/slack/callback`
   - **Save changes**

2. **Update Spotify App:**
   - Go to https://developer.spotify.com/dashboard
   - Select your app → "Edit Settings"
   - Add Redirect URI: `https://trackify.railway.app/oauth/spotify/callback`
   - **Save**

3. **Update Railway Variables Again:**
   - Update `SLACK_REDIRECT_URI` with your actual Railway URL
   - Update `SPOTIFY_REDIRECT_URI` with your actual Railway URL
   - Railway will redeploy automatically

## What I Fixed in the Code

### 1. Updated `application.properties`

**Before:**
```properties
server.port=8080
spring.data.mongodb.uri=mongodb://localhost:27017/trackify
```

**After:**
```properties
server.port=${PORT:8080}  # Railway sets PORT automatically
spring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/trackify}
```

**Why:** Railway uses the `PORT` environment variable, and we need to read MongoDB URI from environment variables.

### 2. Created Comprehensive Documentation

Created these guides to help you:
- **[RAILWAY_ENV_SETUP.md](RAILWAY_ENV_SETUP.md)** - Detailed environment variable setup
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Common errors and solutions
- **[RAILWAY_DEPLOYMENT.md](RAILWAY_DEPLOYMENT.md)** - Updated with warnings about env vars

## Quick Verification Checklist

After setting variables:

- [ ] All 10 environment variables are set in Railway
- [ ] No placeholder values like "your_value_here"
- [ ] ENCRYPTION_SECRET_KEY is at least 32 characters
- [ ] MongoDB connection string is valid
- [ ] Redirect URIs match your Railway domain exactly
- [ ] Railway has redeployed (check Deployments tab)
- [ ] Check logs: `railway logs --follow`

## Expected Result

After setting all variables, your Railway deployment should:

1. ✅ Build successfully (uses Docker with Java 25)
2. ✅ Start successfully (Spring Boot loads all variables)
3. ✅ Connect to MongoDB
4. ✅ Respond to health checks
5. ✅ Show the landing page at your Railway URL

## Testing After Deployment

```bash
# Check if app is running
curl https://your-app.railway.app/

# Check health endpoint (should return 200 OK)
curl https://your-app.railway.app/health

# Check Railway logs
railway logs --follow
```

## Important Notes

### DO NOT Set PORT Manually
Railway automatically sets the `PORT` environment variable. If you set it manually, it may cause conflicts.

### Use the Same Encryption Key
The `ENCRYPTION_SECRET_KEY` must remain the same across deployments. If you change it, all existing encrypted tokens in your database will become unreadable.

### Environment Variables Are Case-Sensitive
Make sure variable names match exactly:
- ✅ `SLACK_CLIENT_ID`
- ❌ `slack_client_id`
- ❌ `Slack_Client_Id`

## Still Getting Errors?

1. **Check Railway Logs:**
   ```bash
   railway logs --follow
   ```

2. **Verify All Variables Are Set:**
   - Railway Dashboard → Variables tab
   - Should see all 10 variables

3. **Check for Typos:**
   - Variable names must match exactly
   - No extra spaces in values
   - No quotes around values (Railway adds them automatically)

4. **Review Detailed Guides:**
   - [RAILWAY_ENV_SETUP.md](RAILWAY_ENV_SETUP.md)
   - [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

## Summary

The error you're seeing is because **Railway needs you to configure environment variables in the dashboard**. The application.properties file references these variables, but they're not set yet in Railway.

**Action Required:**
1. Go to Railway Dashboard → Variables
2. Add all 10 environment variables listed above
3. Replace placeholder values with your actual API keys
4. Save and let Railway redeploy
5. Update Slack and Spotify redirect URIs
6. Test your deployment

---

**Once you set all the environment variables in Railway, the deployment will succeed!** 🚀
