# 🚀 Trackify Quick Start

Get up and running in 5 minutes!

## Prerequisites

✅ Java 21+
✅ MongoDB running
✅ Slack workspace access
✅ Spotify account

## 1. Run Setup Script

```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
```

## 2. Get API Credentials

### Slack App
1. Go to https://api.slack.com/apps
2. Create New App → "Trackify"
3. Copy: Client ID, Client Secret, Signing Secret
4. Add Redirect URI: `http://localhost:8080/oauth/slack/callback`
5. Add Scopes: `users.profile:write`, `users.profile:read`
6. Create Slash Command: `/trackify` → `http://localhost:8080/slack/events`

### Spotify App
1. Go to https://developer.spotify.com/dashboard
2. Create App → "Trackify"
3. Copy: Client ID, Client Secret
4. Add Redirect URI: `http://localhost:8080/oauth/spotify/callback`

## 3. Configure .env

```bash
cp .env.example .env
```

Edit `.env` and add your credentials:

```bash
SLACK_CLIENT_ID=your_slack_client_id
SLACK_CLIENT_SECRET=your_slack_client_secret
SLACK_SIGNING_SECRET=your_slack_signing_secret

SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret

ENCRYPTION_SECRET_KEY=$(openssl rand -base64 32)
```

## 4. Start MongoDB

```bash
# macOS
brew services start mongodb-community

# Docker
docker run -d -p 27017:27017 mongo:7.0

# Linux
sudo systemctl start mongod
```

## 5. Run the App

```bash
# Export environment variables
export $(cat .env | xargs)

# Run the application
./gradlew bootRun
```

Visit http://localhost:8080

## 6. Test It Out

1. Click "Get Started"
2. Authorize Slack
3. Authorize Spotify
4. Play music on Spotify
5. In Slack, type `/trackify status`

## Common Commands

```bash
# Build
./gradlew build

# Run
./gradlew bootRun

# Run with Docker
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop
docker-compose down
```

## Slack Commands

```
/trackify play      # Resume playback
/trackify pause     # Pause playback
/trackify status    # Show status
/trackify enable    # Enable sync
/trackify disable   # Disable sync
/trackify help      # Show help
```

## Troubleshooting

### OAuth fails
- Check redirect URIs match exactly
- Verify credentials in .env
- Check MongoDB is running

### Status not updating
- `/trackify status` to check connection
- Ensure Spotify is playing
- Try `/trackify sync` for manual update

### Build fails
- Check Java version: `java -version`
- Clean build: `./gradlew clean build`

## Project Structure

```
src/main/java/com/trackify/trackify/
├── config/          # Configuration
├── controller/      # REST endpoints
├── model/           # Database entities
├── repository/      # Data access
├── service/         # Business logic
├── slack/           # Slack integration
└── util/            # Utilities

src/main/resources/
├── application.properties
└── templates/       # HTML pages
```

## Next Steps

- Read [SETUP_GUIDE.md](SETUP_GUIDE.md) for detailed setup
- Check [COMMANDS.md](COMMANDS.md) for command reference
- See [README.md](README.md) for complete documentation
- Review [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) for architecture

## Need Help?

1. Check application logs
2. Run `/trackify status` in Slack
3. Review [SETUP_GUIDE.md](SETUP_GUIDE.md) troubleshooting section
4. Open GitHub issue

---

Built with Spring Boot, Slack API, Spotify API ❤️
