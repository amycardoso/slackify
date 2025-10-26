# ⚠️ URGENT: Set Railway Environment Variables NOW

## The Error You're Seeing

```
Could not resolve placeholder 'SLACK_CLIENT_ID' in value "${SLACK_CLIENT_ID}"
```

**This means Railway doesn't have your environment variables configured.**

## 🚨 ACTION REQUIRED: Set Variables in Railway

### Step 1: Open Railway Dashboard

1. Go to: **https://railway.app**
2. Log in to your account
3. Click on your **Trackify** project

### Step 2: Open Variables Tab

1. In the project view, click the **"Variables"** tab (should be at the top)
2. You should see an empty list or existing variables

### Step 3: Add Variables Using Raw Editor (Fastest Method)

1. Click the **"Raw Editor"** button (top right of Variables section)
2. **Copy and paste** the template below
3. **Replace** each `REPLACE_WITH_YOUR_ACTUAL_VALUE` with your real values

```env
SLACK_CLIENT_ID=REPLACE_WITH_YOUR_ACTUAL_VALUE
SLACK_CLIENT_SECRET=REPLACE_WITH_YOUR_ACTUAL_VALUE
SLACK_SIGNING_SECRET=REPLACE_WITH_YOUR_ACTUAL_VALUE
SLACK_REDIRECT_URI=https://your-app-name.railway.app/oauth/slack/callback
SLACK_BOT_TOKEN=

SPOTIFY_CLIENT_ID=REPLACE_WITH_YOUR_ACTUAL_VALUE
SPOTIFY_CLIENT_SECRET=REPLACE_WITH_YOUR_ACTUAL_VALUE
SPOTIFY_REDIRECT_URI=https://your-app-name.railway.app/oauth/spotify/callback

SPRING_DATA_MONGODB_URI=REPLACE_WITH_YOUR_MONGODB_CONNECTION_STRING

ENCRYPTION_SECRET_KEY=REPLACE_WITH_GENERATED_KEY
```

### Step 4: Get Your Actual Values

#### 🟣 Slack Values

1. **Go to:** https://api.slack.com/apps
2. **Select** your Trackify app (or create one if you haven't)
3. **Click** "Basic Information" in the left sidebar
4. **Scroll to** "App Credentials" section
5. **Copy these values:**
   - **Client ID** → Use for `SLACK_CLIENT_ID`
   - **Client Secret** → Click "Show", then copy → Use for `SLACK_CLIENT_SECRET`
   - **Signing Secret** → Click "Show", then copy → Use for `SLACK_SIGNING_SECRET`

#### 🟢 Spotify Values

1. **Go to:** https://developer.spotify.com/dashboard
2. **Select** your Trackify app (or create one)
3. **Click** "Settings" button (top right)
4. **Copy these values:**
   - **Client ID** → Use for `SPOTIFY_CLIENT_ID`
   - **Client Secret** → Click "View client secret", then copy → Use for `SPOTIFY_CLIENT_SECRET`

#### 🗄️ MongoDB Connection String

**Option A: Use Railway's MongoDB Plugin (Recommended)**

1. In Railway project, click **"New"** → **"Database"** → **"Add MongoDB"**
2. Railway will create a MongoDB database
3. Go to the MongoDB service in Railway
4. Go to **"Variables"** tab
5. **Copy** the value of `MONGO_URL` or `MONGODB_URI`
6. **Use it for** `SPRING_DATA_MONGODB_URI`

**Option B: Use MongoDB Atlas (Free Tier)**

1. **Go to:** https://cloud.mongodb.com
2. **Create** a free cluster (if you haven't)
3. **Click** "Connect" → "Connect your application"
4. **Copy** the connection string (looks like: `mongodb+srv://username:password@cluster.mongodb.net/`)
5. **Replace** `<password>` with your actual password
6. **Add** database name: `mongodb+srv://username:password@cluster.mongodb.net/trackify?retryWrites=true&w=majority`
7. **Important:** In MongoDB Atlas, go to Network Access → Add IP Address → **Allow access from anywhere** (`0.0.0.0/0`)

#### 🔐 Generate Encryption Key

**On your local machine, run:**

```bash
openssl rand -base64 32
```

**Copy the output** (should be ~44 characters long) → Use for `ENCRYPTION_SECRET_KEY`

**Example output:**
```
dGhpc2lzYW5leGFtcGxla2V5MTIzNDU2Nzg5MA==
```

### Step 5: Save Variables in Railway

1. After replacing ALL the placeholder values in the Raw Editor
2. **Click** the **"Save"** or **"Update Variables"** button
3. Railway will **automatically redeploy** your application

### Step 6: Verify Deployment

1. Click the **"Deployments"** tab in Railway
2. Watch the latest deployment
3. Check the logs - you should see:
   ```
   Started TrackifyApplication in X.XXX seconds
   ```

4. If you still see the same error, **double-check** that:
   - All 10 variables are set
   - No variables have placeholder text like `REPLACE_WITH_YOUR_ACTUAL_VALUE`
   - Variable names match exactly (case-sensitive!)

### Step 7: Check Logs

```bash
# If you have Railway CLI installed
railway logs --follow

# Or check in Railway Dashboard
# Click "Deployments" → Latest deployment → View logs
```

## ✅ Verification Checklist

Before you click Save, verify:

- [ ] `SLACK_CLIENT_ID` = Your actual Slack client ID (looks like: `123456789.987654321`)
- [ ] `SLACK_CLIENT_SECRET` = Your actual Slack client secret (long string)
- [ ] `SLACK_SIGNING_SECRET` = Your actual Slack signing secret (long string)
- [ ] `SLACK_REDIRECT_URI` = Your Railway URL + `/oauth/slack/callback`
- [ ] `SLACK_BOT_TOKEN` = Empty (just leave blank or empty string)
- [ ] `SPOTIFY_CLIENT_ID` = Your actual Spotify client ID
- [ ] `SPOTIFY_CLIENT_SECRET` = Your actual Spotify client secret
- [ ] `SPOTIFY_REDIRECT_URI` = Your Railway URL + `/oauth/spotify/callback`
- [ ] `SPRING_DATA_MONGODB_URI` = Valid MongoDB connection string (starts with `mongodb://` or `mongodb+srv://`)
- [ ] `ENCRYPTION_SECRET_KEY` = Generated key from `openssl rand -base64 32`

## 🎯 Example with Real Format (NOT real values)

```env
SLACK_CLIENT_ID=1234567890.9876543210
SLACK_CLIENT_SECRET=abc123def456ghi789jkl012mno345pqr678
SLACK_SIGNING_SECRET=xyz789abc123def456ghi789jkl012mno345
SLACK_REDIRECT_URI=https://trackify-production-abc123.railway.app/oauth/slack/callback
SLACK_BOT_TOKEN=

SPOTIFY_CLIENT_ID=a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
SPOTIFY_CLIENT_SECRET=q1w2e3r4t5y6u7i8o9p0a1s2d3f4g5h6
SPOTIFY_REDIRECT_URI=https://trackify-production-abc123.railway.app/oauth/spotify/callback

SPRING_DATA_MONGODB_URI=mongodb+srv://myuser:mypassword@cluster0.abc123.mongodb.net/trackify?retryWrites=true&w=majority

ENCRYPTION_SECRET_KEY=dGhpc2lzYW5leGFtcGxla2V5MTIzNDU2Nzg5MA==
```

## ❌ Common Mistakes to Avoid

1. **Leaving placeholder values** - Make sure to replace ALL `REPLACE_WITH_YOUR_ACTUAL_VALUE` text
2. **Wrong variable names** - Names are case-sensitive! `SLACK_CLIENT_ID` ≠ `slack_client_id`
3. **Extra quotes** - Railway adds quotes automatically, don't wrap values in quotes
4. **Forgetting to click Save** - Changes don't apply until you save
5. **Setting PORT variable** - Don't set PORT, Railway does this automatically
6. **Wrong MongoDB format** - Must start with `mongodb://` or `mongodb+srv://`
7. **MongoDB not allowing connections** - If using Atlas, whitelist `0.0.0.0/0`

## 🆘 Still Getting the Same Error?

If after setting all variables you still see:
```
Could not resolve placeholder 'SLACK_CLIENT_ID'
```

**Try this:**

1. **Screenshot** your Railway Variables tab (hide the secret values)
2. **Verify** variable names match exactly:
   - Not `slack.client-id` → Should be `SLACK_CLIENT_ID`
   - Not `slack_client_id` → Should be `SLACK_CLIENT_ID`
   - Not `slackClientId` → Should be `SLACK_CLIENT_ID`

3. **Force redeploy:**
   - Railway Dashboard → Deployments → Click "..." → "Redeploy"

4. **Check if variables are in the right service:**
   - If you have multiple services in Railway, make sure variables are set on the **Trackify service**, not another service

## 📞 Need Help?

The error message is very clear: Railway cannot find the environment variables. They MUST be set in Railway's Variables tab before the application can start.

**This is a configuration issue, not a code issue.** The application is working correctly - it just needs the variables to be configured in Railway.

---

**Once you set all 10 environment variables in Railway, the deployment will succeed!** 🚀

Then we can proceed to update the redirect URIs once you get your Railway domain.
