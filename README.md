_Last Updated : October 18, 2026_

# Deployment Guide for YABB and it's related services

> [!IMPORTANT]
>
> This guide assumes you already have a working PostgreSQL instance or know how to set up one.

## Step 1: Create a bot in the Discord Dev Portal

Go to the [Discord Developer Portal](https://discord.com/developers/applications) and create an application.

Give it the following intents and permissions:

**Installation Contexts**

1) Guild Install

**Scopes**

1) applications.commands
2) bot

**Permissions**

1) Attach Files
2) Embed Links
3) Mention Everyone
4) Send Messages
5) Send Messages In Threads
6) View Channels

**Privileged Gateway Intents**

1) Server Members Intent

Note down the `TOKEN` since it will be shown only once and will be required in the later steps.

## Step 2: Deploying the API and the Bot

### 2.1: Get Required Variables

| Variable      | Description/Format                                                   | Default Value    | Optional |
|---------------|----------------------------------------------------------------------|------------------|----------|
| `DB_URL`      | jdbc:postgresql://<DATABASE_HOST>:<DATABASE_PORT>/<DATABASE_NAME>    | No Default Value | No       |
| `DB_USERNAME` | Database Username                                                    | No Default Value | No       |
| `DB_PASSWORD` | Database Password                                                    | No Default Value | No       |
| `TOKEN`       | Get from the dev portal                                              | No Default Value | No       |

### 2.2: Deployment

Use the following docker compose setup to deploy both the bot and the API

```yaml
networks:
  yabb-network:

services:
  api:
    container_name: yabb-api
    image: ghcr.io/yetanotherbirthdaybot/yabb-api-native:latest
    environment:
      DB_URL: ${DB_URL}
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
    networks:
      - yabb-network
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:9000/q/health" ]
      interval: 10s
      timeout: 5s
      retries: 3

  bot:
    container_name: yabb-bot
    image: ghcr.io/yetanotherbirthdaybot/yabb:latest
    depends_on:
      api:
        condition: service_healthy
    environment:
      TOKEN: ${TOKEN}
      API_URL: http://yabb-api:8080
    networks:
      - yabb-network
    healthcheck:
      test: [ "CMD", "curl", "-f", "http://localhost:9000/q/health" ]
      interval: 10s
      timeout: 5s
      retries: 3
```
