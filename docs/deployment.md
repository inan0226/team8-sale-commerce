# Deployment

This project uses GitHub Actions for CI/CD.

## CI

`.github/workflows/ci.yml` runs on pull requests and pushes to `main` or `develop`.

- `./gradlew test`
- `./gradlew integrationTest`
- `./gradlew bootJar -x test -x integrationTest`

Failed test reports are uploaded as workflow artifacts.

## CD

`.github/workflows/cd.yml` runs on pushes to `main`, `develop`, version tags, and manual dispatch.

It builds a Docker image and pushes it to GitHub Container Registry:

- `ghcr.io/<owner>/team8-sale-commerce:<short-sha>`
- `ghcr.io/<owner>/team8-sale-commerce:<branch>`
- `ghcr.io/<owner>/team8-sale-commerce:<tag>` for version tags
- `ghcr.io/<owner>/team8-sale-commerce:latest` for version tags

## Optional SSH deployment

The deploy job runs only when these repository secrets exist:

- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`

Optional secrets:

- `DEPLOY_PORT` defaults to `22`
- `DEPLOY_ENV_FILE` defaults to `/opt/team8-sale-commerce/.env`
- `GHCR_USERNAME`
- `GHCR_TOKEN`

The server must have Docker installed and an environment file containing runtime values such as:

```env
DB_URL=jdbc:mysql://mysql-host:3306/team8_sale_commerce
DB_USERNAME=team8
DB_PASSWORD=change-me
JWT_SECRET=change-me-change-me-change-me-change-me
SPRING_DATA_REDIS_HOST=redis-host
SPRING_DATA_REDIS_PORT=6379
WEBSOCKET_ALLOWED_ORIGIN_PATTERNS=*
```

The deploy job runs the container on port `8080`.
