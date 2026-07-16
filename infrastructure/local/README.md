# Local infrastructure

This Docker Compose stack provides development-only dependencies for Nova Platform.

## Start

```bash
cd infrastructure/local
cp .env.example .env
docker compose up -d
```

## Check

```bash
docker compose ps
```

Expected endpoints:

- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- RabbitMQ: `localhost:5672`
- RabbitMQ management: `http://localhost:15672`
- MinIO API: `http://localhost:9000`
- MinIO console: `http://localhost:9001`

## Stop

```bash
docker compose down
```

## Reset all local data

```bash
docker compose down -v
```

Never reuse these example credentials outside local development. Do not commit the generated `.env` file.
