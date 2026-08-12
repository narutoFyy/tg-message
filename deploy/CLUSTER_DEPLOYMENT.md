# Three-node production deployment

The production cluster uses two stateless application nodes and one private data node.

| Role | Private IP | Services |
| --- | --- | --- |
| App A | `10.0.0.3` | Nginx, frontend, backend, WebSocket |
| App B | `10.0.0.2` | Nginx, frontend, backend, WebSocket |
| Data | `10.0.0.4` | MySQL, Redis, NFS uploads |

## Data node

- Mount the dedicated data disk at `/srv/xcard-data`.
- Store MySQL and Redis under `/srv/xcard-data/mysql` and `/srv/xcard-data/redis`.
- Export `/srv/xcard-data/uploads` over NFS only to `10.0.0.2` and `10.0.0.3`.
- Start services with `docker compose --env-file .env -f docker-compose.data.yml up -d`.
- Permit ports `3306`, `6379`, and `2049` only on the private network.

## Application nodes

- Mount `10.0.0.4:/srv/xcard-data/uploads` at `/srv/xcard-uploads`.
- Copy the same production `.env`, Nginx configuration, and TLS certificate files to both nodes.
- Configure one VAPID key pair through `APP_WEB_PUSH_PUBLIC_KEY` and `APP_WEB_PUSH_PRIVATE_KEY`, and use the same pair on both application nodes.
- Add `DATA_HOST=10.0.0.4` to each application `.env`.
- Start services with `docker compose --env-file .env -f docker-compose.app.yml up -d --build`.
- Cloudflare health checks should use `/api/health` and target only the two application public IPs.

MySQL and Redis must never be added as Cloudflare origins or exposed on a public interface.
