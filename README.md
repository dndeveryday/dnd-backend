# D&D Backend

Standalone Java 21 / Spring Boot REST API for customer orders and support.
This repository contains the backend only; it does not include or depend on
the separate React frontend.

## Project structure

Java source is organized by layer under `src/main/java/com/example`:

```text
├── DndBackendApplication.java
├── config/       CORS, security, and OpenAPI configuration
├── controller/   REST endpoints
├── dto/          API request and response models
├── entity/       JPA entities and statuses
├── exception/    API error handling
├── repository/   Spring Data JPA repositories
└── service/      Business logic
```

Database migrations live in `src/main/resources/db/migration`. Flyway applies
them automatically when the application starts.

## Requirements

- Java 21
- Maven 3.9 or newer
- Docker Desktop with Docker Compose

Check the tools in a new PowerShell terminal:

```powershell
java -version
mvn -version
docker --version
docker compose version
```

## Run locally on Windows

Keep Docker Desktop running. Run the commands below from the backend folder.

### 1. Start PostgreSQL

```powershell
cd "C:\Users\bommi\OneDrive\Desktop\keerthi\dnd-backend"
docker compose up -d postgres
docker compose ps
```

PostgreSQL is available at `localhost:5432`. The first startup downloads its
Docker image. Its data persists in the `postgres-data` Docker volume.

### 2. Start local Keycloak

Keycloak provides the local login and JWT issuer used by the secured APIs. If
you already have the `dnd-keycloak` container, start it:

```powershell
docker start dnd-keycloak
```

Otherwise, create it. This command also preserves Keycloak's local data in a
Docker volume:

```powershell
docker run -d --name dnd-keycloak -p 8081:8080 `
  -e KC_BOOTSTRAP_ADMIN_USERNAME=admin `
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin `
  -v dnd-keycloak-data:/opt/keycloak/data `
  quay.io/keycloak/keycloak:26.3.3 start-dev --http-port=8080
```

Open [http://localhost:8081](http://localhost:8081), sign in with the local
admin credentials above, and configure Keycloak:

1. Create a realm named `dnd`.
2. In that realm, create an **OpenID Connect** client with client ID
   `dnd-local`.
3. Turn **Client authentication** off and enable **Direct access grants**.
4. Create a test user. In the user's **Credentials**, set a password and turn
   **Temporary** off. Ensure the user is enabled.
5. To enable customer registration, create a second OIDC client named
  `dnd-backend-provisioner`. Turn **Client authentication** on and enable
  **Service accounts roles**. In the service account's **Role mapping**, assign
  the `manage-users` role from the `realm-management` client. Copy this
  client's secret into the backend environment; do not use the Keycloak admin
  user's password in the backend.

The issuer URL must match this realm exactly:
`http://localhost:8081/realms/dnd`.

### 3. Start the backend

Run these commands in the same PowerShell terminal. The UTC JVM setting avoids
a Windows timezone alias (`Asia/Calcutta`) being rejected by the local
PostgreSQL container:

```powershell
$env:JWT_ISSUER_URI = "http://localhost:8081/realms/dnd"
$env:KEYCLOAK_PROVISIONER_CLIENT_ID = "dnd-backend-provisioner"
$env:KEYCLOAK_PROVISIONER_CLIENT_SECRET = "SET_LOCALLY_FROM_KEYCLOAK"
$env:SPRING_PROFILES_ACTIVE = "local"
$env:SERVER_PORT = "8083"
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Duser.timezone=UTC"
```

Keep this terminal open while using the API. On first startup, Maven downloads
dependencies and Flyway creates/validates the database schema. Stop the
application with `Ctrl+C`.

### 4. Check the API

The health endpoint is public and does not need a token:

```powershell
curl.exe -i "http://localhost:8080/api/v1/health"
```

Swagger UI is at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
The OpenAPI document is at [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs).

## Authentication and first customer

All customer and order endpoints require a valid Keycloak access token. The
backend validates its signature and issuer. It uses the token's `sub` claim to
identify the customer; a client cannot choose a customer ID to access another
customer's records.

In the `local` profile, new users can register before they have a Keycloak
account. Phone ownership is not verified in this temporary flow:

1. `POST /api/v1/customers/registration` with `firstName`, `lastName`,
   `phoneNumber`, `email`, and `password`. Name and email are required by the
   current `dnd` Keycloak realm's user profile.
2. The backend creates a Keycloak account with the phone number as its username,
   then stores a linked customer profile in PostgreSQL. The response contains
   the new profile ID and does not contain the password.
3. The customer signs in using the phone number and password through the
   existing `dnd-local` client to obtain an access token.

Example registration request body:

```json
{
  "firstName": "Test",
  "lastName": "Customer",
  "phoneNumber": "9000000001",
  "email": "test.customer@example.test",
  "password": "ReplaceWithALocalTestPassword!"
}
```

Password-only registration is enabled only with the `local` Spring profile.
Other profiles return `503 Service Unavailable` until phone verification is
implemented. Do not enable `ALLOW_UNVERIFIED_PHONE_REGISTRATION` outside local
development.

In a new PowerShell terminal, request a token using the test user's actual
username and password:

```powershell
$tokenResponse = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8081/realms/dnd/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    client_id  = "dnd-local"
    grant_type = "password"
    username   = "YOUR_TEST_USERNAME"
    password   = "YOUR_TEST_PASSWORD"
    scope      = "openid profile email"
  }

$accessToken = $tokenResponse.access_token
$headers = @{ Authorization = "Bearer $accessToken" }
```

Use the username (not email), and keep the token private. If Keycloak returns
`invalid_grant`, check that the user is enabled, the password is correct and not
temporary, and **Direct access grants** is enabled for the client.

Create or retrieve the signed-in customer's profile by calling:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/customers/me" `
  -Headers $headers
```

The first successful customer API request creates the profile using the
validated token's `sub`, `email`, and `name` claims. There is intentionally no
endpoint that accepts a customer ID to create a profile.

In Swagger UI, click **Authorize** and enter the access token (Swagger adds the
`Bearer` prefix), then try the secured endpoints.

## API reference

All paths are prefixed by `/api/v1`. JSON request bodies use
`Content-Type: application/json`. Except for health and API documentation,
requests require `Authorization: Bearer <access-token>`.

| Method | Path | Authentication | Description |
| --- | --- | --- | --- |
| `GET` | `/health` | Public | Returns `{"status":"UP"}`. |
| `POST` | `/customers/registration` | Public; enabled in local profile only | Creates a Keycloak user and linked customer profile from the supplied name, phone, email, and password; returns `201 Created`. |
| `GET` | `/customers/me` | Required | Gets or creates the current customer's profile. |
| `GET` | `/orders` | Required | Lists the current customer's orders, newest first. |
| `GET` | `/orders/{id}` | Required | Gets one owned order, its items, timeline events, and payment summaries. `{id}` is the order UUID. |
| `GET` | `/admin/orders` | `SUPER_ADMIN` or `OPERATIONS` | Paginated operations order search using the fields currently stored. |
| `POST` | `/conversations` | Required | Starts a conversation; no request body. Returns `201 Created`. |
| `GET` | `/conversations` | Required | Lists the current customer's conversations. |
| `GET` | `/conversations/{id}` | Required | Gets an owned conversation and its messages. |
| `POST` | `/conversations/{id}/messages` | Required | Saves a customer message and, in bot mode, an assistant reply. |
| `POST` | `/tickets` | Required | Creates a ticket and initial history event. Returns `201 Created`. |
| `GET` | `/tickets` | Required | Lists the current customer's tickets. |
| `GET` | `/tickets/{id}` | Required | Gets an owned ticket and event history. |
| `POST` | `/tickets/{id}/close` | Required | Closes an owned ticket and records a history event. |

### Operations order search

`GET /api/v1/admin/orders` is the first read-only endpoint for the Master
Operations Dashboard. It accepts optional `serviceType`, `status`,
`createdFrom`, `createdTo`, `customerId`, and partial `orderNumber` filters.
Results are paginated (50 per page by default) and sorted by newest first.
For example:

```text
/api/v1/admin/orders?serviceType=Food&status=DELIVERED&page=0&size=50
```

The current response includes the order ID/number, service type, status,
customer ID/name, location, amount, currency, and timestamps. It cannot yet
filter by city/zone, merchant, payment/refund state, or SLA because those
fields and domain records are not in the current schema. Keycloak realm roles
`SUPER_ADMIN` and `OPERATIONS` are required; assign one of those roles to the
admin user's token in the `dnd` realm. Customer endpoints remain owner-scoped.

### API inputs, outputs, and app pages

All secured routes below need `Authorization: Bearer <access-token>`. The
`{id}` values are UUIDs returned by earlier calls. These APIs are backend
building blocks; this repository does not include the screens themselves.

| API | Inputs | Output | Page or purpose |
| --- | --- | --- | --- |
| `GET /health` | None; public. | `{"status":"UP"}`. | Deployment check/monitoring, not a customer page. |
| `GET /customers/me` | Bearer token. Creates the profile on first use from token claims. | `id`, `email`, `displayName`, `createdAt`. | Customer account/profile page and app startup identity check. |
| `GET /orders` | Bearer token; no query parameters. | Array of order summaries: ID, order number, service, status, amount, currency, created time. | Customer app's order history. |
| `GET /orders/{id}` | Owned order UUID in the path. | One order with location, items, chronological status events, and payment summaries. | Customer order detail/tracking page. |
| `GET /admin/orders` | Operations-role token; optional `serviceType`, `status`, ISO-8601 `createdFrom`/`createdTo`, `customerId`, `orderNumber`, `page`, and `size`. | Spring page containing matching order rows and pagination totals. | Master Admin/Operations all-orders list and filter panel. |
| `POST /conversations` | Bearer token; no body. | `201` and a new conversation ID/status/timestamps with an empty messages array. | Customer support chat's **New conversation** action. |
| `GET /conversations` | Bearer token; no query parameters. | Array of the customer's conversation summaries. | Customer support chat history/list. |
| `GET /conversations/{id}` | Owned conversation UUID. | Conversation plus its messages (`sender`, `content`, timestamp). | Open chat transcript. |
| `POST /conversations/{id}/messages` | Owned conversation UUID; JSON `{"content":"..."}`. Content is required, non-blank, max 500 chars. | Updated conversation and message history, including a deterministic assistant reply. | Chat composer/send action. The demo assistant can report loaded order status; it is not an AI service. |
| `POST /tickets` | JSON `category`, `summary`, `description`; optional owned `orderId` and `conversationId`; required unique `Idempotency-Key` header (1–128 chars). | `201` and ticket fields/status plus the initial event. | Customer support **Create ticket** form; attach the related order/chat when relevant. |
| `GET /tickets` | Bearer token; no query parameters. | Array of the customer's tickets, each with its event history. | Customer support ticket list. |
| `GET /tickets/{id}` | Owned ticket UUID. | One ticket with current status and chronological events. | Ticket detail page. |
| `POST /tickets/{id}/close` | Owned ticket UUID; no body. | Updated ticket with `CLOSED` status and a close event. | Customer ticket detail **Close ticket** action. |

Example order summary from the demo data:

```json
{
  "id": "10000000-0000-0000-0000-000000000001",
  "orderNumber": "DEMO-FOOD-1001",
  "serviceType": "Food",
  "status": "DELIVERED",
  "totalAmount": 490.00,
  "currency": "INR"
}
```

### Local demo data

The local Keycloak realm has synthetic users `demo.customer` and `demo.ops`;
the latter has the `OPERATIONS` realm role. Use their local-only passwords
when following the token steps above. Do not reuse these demo accounts outside
local development. The customer has these sample orders:

| Order number | Service | Status | Amount/payment |
| --- | --- | --- | --- |
| `DEMO-FOOD-1001` | Food | `DELIVERED` | INR 490.00, `PAID`; six timeline events. |
| `DEMO-MED-2001` | Medicine | `CONFIRMED` | INR 440.00, `PENDING`. |
| `DEMO-HANDS-3001` | Extra Hands | `IN_TRANSIT` | INR 1,200.00, `AUTHORIZED`. |

The current local demo also has a conversation that asks about
`DEMO-FOOD-1001` and a linked example support ticket. Orders are intentionally
not created through a public API; for local testing, load the seed against a
customer profile created by `GET /customers/me`:

```powershell
$baseUrl = "http://localhost:8080"
$customerId = (Invoke-RestMethod -Uri "$baseUrl/api/v1/customers/me" -Headers $headers).id
docker cp .\scripts\seed-local-demo.sql dnd-backend-postgres:/tmp/seed-local-demo.sql
docker exec dnd-backend-postgres psql -v ON_ERROR_STOP=1 -U app -d app `
  -v "customer_id=$customerId" -f /tmp/seed-local-demo.sql
```

In this running workspace the backend is on port `8083` because port `8080`
was already occupied; set `$baseUrl = "http://localhost:8083"` for the current
instance. The seed uses fixed IDs and `ON CONFLICT DO NOTHING`, so rerunning it
does not duplicate sample rows. `docker compose down -v` deletes the database
volume and its demo data.

### Product boundaries in the DND brief

The brief describes multiple client applications backed by shared platform
services. This repository currently contains only the Spring Boot API; it does
not contain any of the client applications below.

| Application | Main users and purpose | Current state in this workspace |
| --- | --- | --- |
| Customer app | Customers browse/order Food and Medicine, request Extra Hands, track work, manage their profile, and contact support. | No frontend. The backend has customer identity, read-only order lookup, conversations, and tickets. |
| Admin and Operations Dashboard | Staff search all services, monitor orders and SLAs, review customer history, manage support, KYC, refunds, finance, configuration, and reports according to role. | No frontend. A first role-protected, read-only order search API is available; most dashboard data/workflows are not implemented. |
| Merchant portal | Restaurants and pharmacies manage their own outlet/catalog, accept orders, update preparation/packing, and review settlement/support information. | Not implemented; merchant and outlet domains are absent. |
| Partner app | Delivery and Extra Hands partners complete onboarding/KYC, manage availability, receive work, update task status, and view earnings. | Not implemented; partner, task, and KYC domains are absent. |

Authentication, order/request identity, timelines, payments, notifications,
support, city/zone configuration, audit, and authorization should remain shared
backend capabilities rather than being reimplemented separately in each app.
The next backend foundations are the common service/location/merchant/partner
data model, immutable operational events, scoped admin permissions, and
configurable SLA/notification rules. The brief's full dashboard, payment,
refund, KYC, and multi-service requirements are not complete in this initial
slice.

### Start a conversation and send a message

Use the `$headers` variable from the token instructions:

```powershell
$conversation = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/conversations" `
  -Headers $headers

$messageBody = @{ content = "Where is my order?" } | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/conversations/$($conversation.id)/messages" `
  -Headers $headers `
  -ContentType "application/json" `
  -Body $messageBody
```

`content` is required and limited to 500 characters. The current deterministic
assistant can report an order's status if orders have been loaded. It returns a
simple fallback for other questions; it is not an AI service and does not yet
include the demo application's FAQ content.

### Create a support ticket

`category`, `summary`, and `description` are required. `orderId` and
`conversationId` are optional UUIDs and, if supplied, must belong to the
authenticated customer. Provide a unique `Idempotency-Key` header (1–128
characters); retrying with the same key for the same customer returns the
existing ticket.

```powershell
$ticketBody = @{
  category       = "general"
  summary        = "Testing the support API"
  description    = "Please help with my question."
  orderId        = $null
  conversationId = $conversation.id
} | ConvertTo-Json

$ticketHeaders = $headers.Clone()
$ticketHeaders["Idempotency-Key"] = [guid]::NewGuid().ToString()

$ticket = Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/tickets" `
  -Headers $ticketHeaders `
  -ContentType "application/json" `
  -Body $ticketBody

$ticket
```

Use `category = "human_support"` with a conversation ID to mark the conversation
as being handed to a human. Agent assignment/replies are not implemented.

### Orders and sample data

The customer API is read-only for orders and payments. No orders are created by
the migrations or API, so `GET /orders` is expected to return an empty list on a
fresh database. Orders must be loaded by a trusted integration or backend
process before order lookup and assistant tracking responses can return order
data. Payment records contain provider references only, not card details.

## Database

The default local PostgreSQL connection is:

| Setting | Default |
| --- | --- |
| Host and port | `localhost:5432` |
| Database | `app` |
| Username | `app` |
| Password | `app` |

Connect to these settings in DBeaver to browse the `public` schema and its
tables. `V1` is the original starter migration and is retained unchanged for
Flyway checksum safety; `V2` creates the support domain tables. The old `items`
table from `V1` is unused.

The `postgres-data` volume preserves the data when the PostgreSQL container is
stopped. `docker compose down -v` deletes the volume and all its database data;
do not use `-v` if you need to keep the data.

## Configuration

Set environment variables in PowerShell before starting the backend. For
example, `$env:JWT_ISSUER_URI = "http://localhost:8081/realms/dnd"` applies only
to the current terminal.

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Backend HTTP port. |
| `DB_URL` | `jdbc:postgresql://localhost:5432/app` | JDBC connection URL. |
| `DB_USERNAME` | `app` | PostgreSQL username. |
| `DB_PASSWORD` | `app` | PostgreSQL password. |
| `JWT_ISSUER_URI` | Required | OIDC issuer used to validate access tokens; local Keycloak: `http://localhost:8081/realms/dnd`. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Comma-separated browser origins permitted by CORS. |
| `DB_NAME` | `app` | Database name for the PostgreSQL Docker container. |
| `DB_PORT` | `5432` | Host port mapped to PostgreSQL. |

`DB_NAME` and `DB_PORT` configure Docker Compose; the Spring datasource uses
`DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

## Troubleshooting

- **`mvn` is not recognized:** install Maven, add its `bin` directory to your
  Windows user `Path`, then close and reopen VS Code.
- **JWT issuer placeholder is missing:** set `JWT_ISSUER_URI` in the same
  terminal before running Maven.
- **Cannot connect to the issuer:** start the Keycloak container, create the
  `dnd` realm, and make sure the issuer URL is exactly
  `http://localhost:8081/realms/dnd`.
- **PostgreSQL reports `Asia/Calcutta` as an invalid timezone:** run the
  application with the documented `-Duser.timezone=UTC` JVM argument.
- **401 Unauthorized:** use a valid Keycloak access token, not a public key;
  health does not require a token.
- **Empty orders:** expected until an order-provider integration or trusted
  process loads them.



# To Run application after all setUp :
docker compose up -d postgres
docker start dnd-keycloak

$env:JWT_ISSUER_URI = "http://localhost:8081/realms/dnd"
$env:SERVER_PORT = "8083"
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Duser.timezone=UTC"