# Railway Environment Variables Debug Guide

## Problem

You're getting:
```
Could not resolve placeholder 'SLACK_CLIENT_ID' in value "${SLACK_CLIENT_ID}"
```

Even though you claim `SLACK_CLIENT_ID` is set in Railway.

## Things to Check

### 1. Are Variables Set in the RIGHT Service?

Railway projects can have multiple services (web app, database, etc.). **You must set variables in your Trackify web service, not at the project level or in another service.**

**Steps to verify:**

1. Go to Railway Dashboard
2. Open your Trackify project
3. You should see **multiple boxes/cards** - one for each service
4. **Click on the service that deploys your code** (usually the one connected to GitHub)
5. **Click the "Variables" tab** (should be tabs like: Settings, Variables, Deployments, Metrics)
6. Check if all 10 variables are listed there

**Common mistake:** Setting variables in a MongoDB service or at project level instead of the web service.

### 2. Variable Names - Exact Match Required

Railway variable names are **case-sensitive** and must match exactly:

✅ **Correct:**
```
SLACK_CLIENT_ID
SLACK_CLIENT_SECRET
SLACK_SIGNING_SECRET
SPOTIFY_CLIENT_ID
SPOTIFY_CLIENT_SECRET
SPRING_DATA_MONGODB_URI
ENCRYPTION_SECRET_KEY
```

❌ **Wrong:**
```
slack.client-id          (uses dots and dashes)
slack_client_id          (lowercase)
Slack_Client_Id          (mixed case)
SLACK_CLIENT_lD          (lowercase L instead of uppercase i)
```

### 3. Check Railway's Variable Scope

Railway has two types of variables:

**Service Variables** (what you need):
- Set per service
- Only available to that specific service
- This is where your SLACK_CLIENT_ID should be

**Shared Variables**:
- Set at project level
- Shared across all services
- These should work too, but less common

### 4. Verify Variables Are Actually There

**In Railway Dashboard:**

1. Click your Trackify service (the one deploying from GitHub)
2. Click "Variables" tab
3. You should see a list like:

```
SLACK_CLIENT_ID             1234567890.987...
SLACK_CLIENT_SECRET         abc123def456ghi...
SLACK_SIGNING_SECRET        xyz789abc123def...
SLACK_REDIRECT_URI          https://...
SPOTIFY_CLIENT_ID           a1b2c3d4e5f6...
SPOTIFY_CLIENT_SECRET       q1w2e3r4t5y6...
SPOTIFY_REDIRECT_URI        https://...
SPRING_DATA_MONGODB_URI     mongodb+srv://...
ENCRYPTION_SECRET_KEY       dGhpc2lz...
```

If you **don't** see them, they're not set for this service.

### 5. Check If Using Railway CLI vs Dashboard

If you set variables via Railway CLI, they might not show up in dashboard immediately.

**Try setting via dashboard:**
1. Railway Dashboard → Your Project → Your Service → Variables
2. Use Raw Editor to paste all variables
3. Click Save

### 6. Force Rebuild

Sometimes Railway needs a fresh deployment after adding variables:

1. Go to Deployments tab
2. Click the "..." menu on latest deployment
3. Click "Redeploy"

OR

1. Make a small change to your code (add a comment)
2. Commit and push
3. Railway will rebuild

### 7. Check Railway Logs for Variable Hints

In the deployment logs, just before the error, you might see:

```
Loading environment variables...
```

If you see this, it means Railway is trying to load them. If you don't see this, Railway might not be injecting them.

### 8. Railway Service Settings

Check if your service is configured correctly:

1. Click your Trackify service
2. Click "Settings" tab
3. Under "Environment", check:
   - **Start Command**: Should be blank (Dockerfile handles this)
   - **Build Command**: Should be blank (Dockerfile handles this)
   - **Root Directory**: Should be `/` or blank
   - **Dockerfile Path**: Should be `/Dockerfile` or blank (auto-detected)

### 9. Test with a Simple Variable

Add a test variable to verify Railway is working:

1. Add variable: `TEST_VAR=hello`
2. Temporarily update Dockerfile ENTRYPOINT:
   ```dockerfile
   ENTRYPOINT ["sh", "-c", "echo TEST_VAR=$TEST_VAR && java -jar app.jar"]
   ```
3. Deploy
4. Check logs - you should see `TEST_VAR=hello`

If you see `TEST_VAR=` (empty), Railway isn't injecting variables.

### 10. Common Railway Issues

**Issue: Using Railway V1 vs V2**
- Railway upgraded their platform
- V1 and V2 handle variables differently
- Check if you're on the new platform

**Issue: Service is in "Sleeping" state**
- Railway free tier has limits
- Check if service is active

**Issue: Multiple Railway Projects**
- Make sure you're looking at the correct project
- Check the URL - should match your deployment

## Debugging Commands

If you have Railway CLI installed:

```bash
# List all variables for your service
railway variables

# Check which project/service you're in
railway status

# View logs
railway logs --follow
```

## Next Steps

1. **Screenshot your Railway Variables tab** (blur the secret values)
2. **Verify you're in the right service** (the one connected to GitHub)
3. **Check variable names match exactly**
4. **Try using Raw Editor** to set all variables at once
5. **Force a redeploy** after setting variables

## Still Not Working?

If you've verified all of the above and it's still failing, try this:

### Alternative: Use Spring Profiles

Create `src/main/resources/application-railway.properties`:

```properties
# Railway-specific configuration
slack.client-id=${SLACK_CLIENT_ID:#{null}}
slack.client-secret=${SLACK_CLIENT_SECRET:#{null}}
slack.signing-secret=${SLACK_SIGNING_SECRET:#{null}}
spotify.client-id=${SPOTIFY_CLIENT_ID:#{null}}
spotify.client-secret=${SPOTIFY_CLIENT_SECRET:#{null}}
trackify.encryption.secret-key=${ENCRYPTION_SECRET_KEY:#{null}}
```

Then in Railway, add:
```
SPRING_PROFILES_ACTIVE=railway
```

### Alternative: Use Railway's Template Variables

Some Railway templates use different variable injection methods. Check if Railway has a "Internal Variables" section that auto-generates variables.

---

**The most common issue is that variables are set in the wrong service or the service hasn't redeployed since adding them.**
