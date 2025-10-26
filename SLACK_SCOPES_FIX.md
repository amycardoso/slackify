# Slack OAuth Scopes Configuration

## Important: User Token Scopes vs Bot Token Scopes

For Trackify to work properly, you need to configure **User Token Scopes**, NOT Bot Token Scopes.

## Correct Configuration in Slack App

### Step-by-Step Setup:

1. **Go to**: https://api.slack.com/apps
2. **Select your app** (Trackify)
3. **Navigate to**: "OAuth & Permissions" in the left sidebar

### User Token Scopes (REQUIRED) ✅

Scroll down to **"User Token Scopes"** section and add:

```
✅ users.profile:write
✅ users.profile:read
```

**Why User Token Scopes?**
- These scopes allow users to update their **own** profile/status
- Bot tokens cannot modify user profiles (only bot's own profile)
- Trackify needs to act on behalf of the user, not as a bot

### Bot Token Scopes (Optional)

You can optionally add these if you want bot features:

```
⚠️ chat:write (optional - for sending messages)
⚠️ commands (automatically added when you create slash commands)
```

**Note:** Bot scopes are NOT required for the core functionality of updating user status.

## Valid Slack Scopes for User Profiles

The following scopes ARE valid for Slack's User Token Scopes:

| Scope | Permission | Purpose |
|-------|-----------|---------|
| `users.profile:write` | Write user's profile | Update user's status, display name, etc. |
| `users.profile:read` | Read user's profile | View user's profile information |
| `users:read` | View users | View people in a workspace |
| `users:read.email` | View email addresses | View email addresses of people |

## Common Mistakes ❌

### ❌ Wrong: Adding scopes to Bot Token Scopes
```
Bot Token Scopes:
  ❌ users.profile:write (This won't work!)
  ❌ users.profile:read (This won't work!)
```

### ✅ Correct: Adding scopes to User Token Scopes
```
User Token Scopes:
  ✅ users.profile:write
  ✅ users.profile:read
```

## OAuth Flow Type

Trackify uses **OAuth 2.0 User Token flow**, which means:

1. User authorizes the app
2. App receives a **user token** (not bot token)
3. App uses user token to update the user's own profile
4. Each user has their own token

## Testing Your Configuration

After setting up the scopes correctly:

1. **Reinstall/Reinstall the app** to your workspace (if already installed)
2. **Test OAuth flow**: Visit `http://localhost:8080/oauth/slack`
3. **Check token**: The response should include `authed_user.access_token`
4. **Verify scopes**: Check that the token has the correct scopes

## Troubleshooting

### Error: "missing_scope" or "not_authed"

**Cause**: Using Bot Token instead of User Token

**Fix**:
1. Check that scopes are in **User Token Scopes** section
2. Reinstall the app to get new tokens
3. Verify `OAuthV2AccessResponse.getAuthedUser().getAccessToken()` is being used (not `getBotToken()`)

### Error: "invalid_auth"

**Cause**: Expired or invalid token

**Fix**:
1. Delete user from database
2. Re-authorize through OAuth flow
3. New token will be issued

## Configuration in application.properties

The configuration in `application.properties` is correct:

```properties
spring.security.oauth2.client.registration.slack.scope=users.profile:write,users.profile:read
```

This tells Spring Security to request these scopes during OAuth.

## Slack API Documentation

Official docs:
- [OAuth Scopes](https://api.slack.com/scopes)
- [users.profile:write](https://api.slack.com/scopes/users.profile:write)
- [users.profile:read](https://api.slack.com/scopes/users.profile:read)
- [users.profile.set API](https://api.slack.com/methods/users.profile.set)

## Summary

✅ **DO**: Add `users.profile:write` and `users.profile:read` to **User Token Scopes**
❌ **DON'T**: Add these scopes to Bot Token Scopes
✅ **DO**: Use the user's access token (not bot token) in your code
✅ **DO**: Reinstall app after changing scopes

---

If you've configured this correctly and still have issues, please check:
1. ✅ Scopes are in **User Token Scopes** section (not Bot Token Scopes)
2. ✅ App has been reinstalled after adding scopes
3. ✅ Using `authed_user.access_token` in the code (check OAuthController.java)
4. ✅ Slack App is using OAuth v2 (not legacy OAuth v1)
