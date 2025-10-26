# 🚂 Railway Deployment Guide for Trackify

This guide will help you deploy Trackify to Railway.app.

## ⚠️ CRITICAL: Environment Variables Must Be Set First!

**The application will NOT start without environment variables configured in Railway.**

See [RAILWAY_ENV_SETUP.md](RAILWAY_ENV_SETUP.md) for detailed environment variable setup instructions.

## Prerequisites

- Railway account (sign up at [railway.app](https://railway.app))
- GitHub repository with Trackify code
- MongoDB Atlas account (or Railway MongoDB plugin)
- Slack App configured
- Spotify App configured

## Step 1: Prepare Your Environment Variables

**⚠️ IMPORTANT:** You MUST set these in Railway's dashboard BEFORE deploying!

You'll need these environment variables for Railway:

```bash
# Slack Configuration
SLACK_CLIENT_ID=your_slack_client_id
SLACK_CLIENT_SECRET=your_slack_client_secret
SLACK_SIGNING_SECRET=your_slack_signing_secret
SLACK_REDIRECT_URI=https://your-app.railway.app/oauth/slack/callback
SLACK_BOT_TOKEN=

# Spotify Configuration
SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
SPOTIFY_REDIRECT_URI=https://your-app.railway.app/oauth/spotify/callback

# MongoDB Configuration (if using MongoDB Atlas)
SPRING_DATA_MONGODB_URI=mongodb+srv://username:password@cluster.mongodb.net/trackify?retryWrites=true&w=majority

# Encryption Configuration
ENCRYPTION_SECRET_KEY=your_generated_secret_key

# Server Configuration (Railway sets this automatically - DO NOT set manually)
# PORT=8080  # Railway will set this automatically
```

**Note:** Do NOT set the `PORT` variable manually - Railway sets it automatically.

## Step 2: Set Up MongoDB

### Option A: Railway MongoDB Plugin

1. In Railway dashboard, click "New" → "Database" → "Add MongoDB"
2. Copy the connection string
3. Set `SPRING_DATA_MONGODB_URI` to the connection string

### Option B: MongoDB Atlas

1. Create cluster at [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas)
2. Create database user
3. Whitelist all IPs (0.0.0.0/0) for Railway
4. Get connection string
5. Set `SPRING_DATA_MONGODB_URI` in Railway

## Step 3: Deploy to Railway

### Method 1: Deploy from GitHub (Recommended)

1. **Go to Railway Dashboard**: https://railway.app
2. **Click "New Project"**
3. **Select "Deploy from GitHub repo"**
4. **Authorize Railway** to access your GitHub
5. **Select your repository** (trackify)
6. **Railway will auto-detect** the Dockerfile and start building

### Method 2: Railway CLI

```bash
# Install Railway CLI
npm install -g @railway/cli

# Login to Railway
railway login

# Initialize project
railway init

# Deploy
railway up
```

## Step 4: Configure Environment Variables

1. **In Railway Dashboard**, select your project
2. **Go to "Variables" tab**
3. **Add all environment variables** from Step 1
4. **Click "Deploy"** to redeploy with new variables

### Quick Add Variables:

You can bulk add variables:

```bash
# In Railway dashboard, click "Raw Editor" and paste:
SLACK_CLIENT_ID=xxx
SLACK_CLIENT_SECRET=xxx
SLACK_SIGNING_SECRET=xxx
SLACK_REDIRECT_URI=https://your-app.railway.app/oauth/slack/callback
SPOTIFY_CLIENT_ID=xxx
SPOTIFY_CLIENT_SECRET=xxx
SPOTIFY_REDIRECT_URI=https://your-app.railway.app/oauth/spotify/callback
SPRING_DATA_MONGODB_URI=mongodb+srv://...
ENCRYPTION_SECRET_KEY=xxx
```

## Step 5: Get Your Railway URL

1. In Railway dashboard, go to **"Settings" tab**
2. Under **"Domains"**, click **"Generate Domain"**
3. Copy your URL (e.g., `trackify.railway.app`)
4. Update your redirect URIs with this URL

## Step 6: Update Slack App Configuration

1. **Go to**: https://api.slack.com/apps
2. **Select your Trackify app**
3. **Update OAuth & Permissions**:
   - Redirect URL: `https://your-app.railway.app/oauth/slack/callback`
4. **Update Slash Commands**:
   - Request URL: `https://your-app.railway.app/slack/events`
5. **Reinstall app** to workspace

## Step 7: Update Spotify App Configuration

1. **Go to**: https://developer.spotify.com/dashboard
2. **Select your Trackify app**
3. **Edit Settings**
4. **Add Redirect URI**: `https://your-app.railway.app/oauth/spotify/callback`
5. **Save**

## Step 8: Test Your Deployment

1. **Visit**: `https://your-app.railway.app`
2. **Click** "Get Started - Connect with Slack"
3. **Authorize** Slack and Spotify
4. **Test in Slack**: `/trackify status`

## Troubleshooting

### Build Fails with Java Version Error

**Error**: `No matching toolchains found for requested specification: {languageVersion=25}`

**Fix**: This is already fixed in `build.gradle` (Java 21). Pull latest changes.

### Database Connection Failed

**Fix**:
- Check MongoDB connection string in Railway variables
- Ensure IP whitelist includes `0.0.0.0/0` (for MongoDB Atlas)
- Verify database user has read/write permissions

### Application Crashes on Startup

**Check logs**:
```bash
# View logs in Railway dashboard or CLI
railway logs
```

**Common issues**:
- Missing environment variables
- Invalid MongoDB connection string
- Port configuration (Railway sets `PORT` automatically)

### OAuth Redirect Mismatch

**Fix**:
- Ensure redirect URIs in Slack/Spotify apps match Railway URL exactly
- Use HTTPS (Railway provides this automatically)
- Check `SLACK_REDIRECT_URI` and `SPOTIFY_REDIRECT_URI` in Railway variables

### Health Check Fails

**Fix**: Update `application.properties` if needed:
```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.show-details=always
```

## Railway Configuration Tips

### 1. Custom Domain (Optional)

In Railway dashboard:
1. Go to "Settings" → "Domains"
2. Click "Custom Domain"
3. Add your domain (e.g., `trackify.yourdomain.com`)
4. Update DNS records as instructed
5. Update redirect URIs in Slack and Spotify apps

### 2. Automatic Deployments

Railway automatically deploys when you push to your main branch.

**To disable**:
1. Go to "Settings" → "Triggers"
2. Disable "Deploy on push"

### 3. Resource Usage

**Free tier limits**:
- $5 of usage per month
- Shared resources
- Auto-sleep after inactivity

**For production**:
- Upgrade to Hobby plan ($5/month)
- Add more resources if needed

### 4. Monitoring

**View logs**:
```bash
railway logs --follow
```

**Or in dashboard**:
- Go to "Deployments" tab
- Click on a deployment
- View build and runtime logs

## Environment Variables Reference

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `SLACK_CLIENT_ID` | Yes | Slack app client ID | `123456789.987654321` |
| `SLACK_CLIENT_SECRET` | Yes | Slack app client secret | `abc123...` |
| `SLACK_SIGNING_SECRET` | Yes | Slack app signing secret | `def456...` |
| `SLACK_REDIRECT_URI` | Yes | Slack OAuth redirect | `https://app.railway.app/oauth/slack/callback` |
| `SPOTIFY_CLIENT_ID` | Yes | Spotify app client ID | `abc123...` |
| `SPOTIFY_CLIENT_SECRET` | Yes | Spotify app client secret | `def456...` |
| `SPOTIFY_REDIRECT_URI` | Yes | Spotify OAuth redirect | `https://app.railway.app/oauth/spotify/callback` |
| `SPRING_DATA_MONGODB_URI` | Yes | MongoDB connection string | `mongodb+srv://...` |
| `ENCRYPTION_SECRET_KEY` | Yes | Encryption key for tokens | `base64_encoded_key` |
| `PORT` | No | Port (auto-set by Railway) | `8080` |

## Deployment Checklist

Before going live:

- [ ] MongoDB database created and accessible
- [ ] All environment variables set in Railway
- [ ] Slack app redirect URIs updated
- [ ] Spotify app redirect URIs updated
- [ ] Slack app reinstalled to workspace
- [ ] Application builds successfully
- [ ] Application starts without errors
- [ ] Health check endpoint responds
- [ ] OAuth flows work (Slack and Spotify)
- [ ] Slash commands work in Slack
- [ ] Music sync functionality tested

## Cost Estimate

**Free Tier**:
- Railway: $5 free credit/month
- MongoDB Atlas: Free M0 cluster
- **Total**: Free (for small usage)

**Production**:
- Railway Hobby: $5/month
- MongoDB Atlas M2: $9/month
- **Total**: ~$14/month

## Support

If you encounter issues:

1. Check Railway logs: `railway logs`
2. Check MongoDB connection
3. Verify environment variables
4. Test OAuth flows locally first
5. Review Slack/Spotify app configurations

## Additional Resources

- [Railway Documentation](https://docs.railway.app/)
- [MongoDB Atlas Documentation](https://docs.atlas.mongodb.com/)
- [Slack API Documentation](https://api.slack.com/)
- [Spotify Web API Documentation](https://developer.spotify.com/documentation/web-api)

---

Happy deploying! 🚂🎵
