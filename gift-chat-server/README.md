# CardBrother Server

`gift-chat-server` is the Spring Boot backend for the CardBrother production MVP.

It now includes:

- JWT-based authentication
- Dev and prod profiles
- Flyway database migration
- MySQL-ready production configuration
- H2-backed local development profile
- REST APIs for rates, friends, support chat, transactions, blacklist, and uploads
- WebSocket endpoints for realtime chat delivery

## Runtime profiles

- `dev`
  - H2 in-memory database
  - demo seed data loaded from `data-dev.sql`
  - H2 console enabled
  - demo fallback allowed when no token is present
- `prod`
  - MySQL datasource required
  - demo fallback disabled
  - intended for Docker / Linux deployment

## Key API areas

- Auth
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- Rates
  - `GET /api/rates`
  - `POST /api/admin/rates`
  - `POST /api/admin/rates/{rateId}/status`
- Friends
  - `GET /api/friends`
  - `GET /api/friends/search?username=...`
  - `GET /api/friends/requests`
  - `POST /api/friends/requests`
  - `POST /api/friends/requests/{friendshipId}/accept`
  - `POST /api/friends/requests/{friendshipId}/reject`
  - `POST /api/friends/{friendshipId}/messages`
- Support
  - `GET /api/support/conversations`
  - `POST /api/support/conversations/{conversationId}/messages`
- Transactions
  - `GET /api/transactions`
  - `GET /api/transactions/{transactionId}`
  - `POST /api/transactions`
  - `POST /api/transactions/{transactionId}/status`
- Uploads
  - `POST /api/uploads/images`
  - `POST /api/uploads/voices`
- Blacklist
  - `GET /api/blacklist`
  - `POST /api/blacklist`
- WebSocket
  - `WS /ws/chat/friend/{friendshipId}?token=...`
  - `WS /ws/chat/support/{conversationId}?token=...`

## Local development

```bash
mvn spring-boot:run
```

Useful commands:

- `mvn test`
- `mvn -DskipTests package`

Local services:

- API: [http://127.0.0.1:8081](http://127.0.0.1:8081)
- H2 console: [http://127.0.0.1:8081/h2-console](http://127.0.0.1:8081/h2-console)

## Production deployment

Production deployment files live under [deploy](/D:/cyproject/tg-message/deploy/DEPLOYMENT.md).

The server Docker image is defined in [Dockerfile](/D:/cyproject/tg-message/gift-chat-server/Dockerfile).
