# Railway Quick Start - TL;DR

## 🚨 The Error You're Getting

```
Could not resolve placeholder 'SLACK_CLIENT_ID' in value "${SLACK_CLIENT_ID}"
```

**Reason:** Environment variables are NOT set in Railway's dashboard.

## ✅ Solution in 3 Steps

### Step 1: Go to Railway Variables

1. Open https://railway.app
2. Select your Trackify project
3. Click **"Variables"** tab
4. Click **"Raw Editor"**

### Step 2: Paste These Variables

Replace `your_value_here` with your actual values:

```bash
SLACK_CLIENT_ID=your_slack_client_id
SLACK_CLIENT_SECRET=your_slack_client_secret
SLACK_SIGNING_SECRET=your_slack_signing_secret
SLACK_REDIRECT_URI=https://your-app.railway.app/oauth/slack/callback
SLACK_BOT_TOKEN=

SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
SPOTIFY_REDIRECT_URI=https://your-app.railway.app/oauth/spotify/callback

SPRING_DATA_MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/trackify?retryWrites=true&w=majority

ENCRYPTION_SECRET_KEY=generate_with_openssl_rand_base64_32
```

### Step 3: Save and Redeploy

1. Click **"Save"**
2. Railway will auto-redeploy
3. Check logs: `railway logs --follow`

## 📝 Where to Get Values

| Variable | Where to Get It |
|----------|----------------|
| `SLACK_CLIENT_ID` | https://api.slack.com/apps → Your App → Basic Information |
| `SLACK_CLIENT_SECRET` | https://api.slack.com/apps → Your App → Basic Information |
| `SLACK_SIGNING_SECRET` | https://api.slack.com/apps → Your App → Basic Information |
| `SPOTIFY_CLIENT_ID` | https://developer.spotify.com/dashboard → Your App |
| `SPOTIFY_CLIENT_SECRET` | https://developer.spotify.com/dashboard → Your App → Settings |
| `SPRING_DATA_MONGODB_URI` | Railway MongoDB Plugin OR MongoDB Atlas |
| `ENCRYPTION_SECRET_KEY` | Generate: `openssl rand -base64 32` |

## 🗄️ MongoDB Options

**Option A: Railway MongoDB Plugin (Easiest)**
1. In Railway: "New" → "Database" → "Add MongoDB"
2. Copy the `MONGO_URL` variable
3. Use it for `SPRING_DATA_MONGODB_URI`

**Option B: MongoDB Atlas (Free Tier)**
1. Create cluster at https://cloud.mongodb.com
2. Whitelist all IPs: `0.0.0.0/0`
3. Get connection string

## 🔑 Generate Encryption Key

```bash
openssl rand -base64 32
```

Copy the output → use for `ENCRYPTION_SECRET_KEY`

## ✅ Checklist

After setting variables:

- [ ] All 10 variables are set in Railway
- [ ] No placeholder values
- [ ] `ENCRYPTION_SECRET_KEY` is 32+ characters
- [ ] MongoDB connection string is valid
- [ ] Railway has redeployed
- [ ] Logs show no errors: `railway logs --follow`

## 🎯 After First Deployment

1. Get your Railway URL (e.g., `trackify.railway.app`)
2. Update Slack app redirect URL
3. Update Spotify app redirect URL
4. Update `SLACK_REDIRECT_URI` in Railway variables
5. Update `SPOTIFY_REDIRECT_URI` in Railway variables
6. Railway will auto-redeploy

## 📖 Detailed Guides

- **Environment Variables:** [RAILWAY_ENV_SETUP.md](RAILWAY_ENV_SETUP.md)
- **Full Deployment:** [RAILWAY_DEPLOYMENT.md](RAILWAY_DEPLOYMENT.md)
- **Troubleshooting:** [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- **Specific Fix:** [RAILWAY_ENV_FIX.md](RAILWAY_ENV_FIX.md)

## 🆘 Still Not Working?

Check logs first:
```bash
railway logs --follow
```

Common issues:
- Typo in variable names (case-sensitive!)
- MongoDB connection string invalid
- Forgot to click "Save" in Railway
- PORT variable set manually (don't do this!)

---

**Once you set all environment variables, the deployment will work!** 🚀
