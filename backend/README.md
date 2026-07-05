# CraftIoT Production Backend Node Platform

A production-grade, highly available NestJS microservices platform designed to orchestrate physical IoT device telemetry streams, manage access delegations, trigger autonomous automation rules, distribute OTA firmwares, and provide context-aware AI assistant responses using Gemini.

---

## 🏗️ System Architecture & Stack

- **Framework**: NestJS (TypeScript)
- **Database**: PostgreSQL with Prisma ORM
- **In-Memory Store**: Redis (Distributed Caching & Push Alert Storage)
- **Messaging Protocol**: MQTT (EMQX Message Broker)
- **API Documentation**: OpenAPI / Swagger
- **Authentication**: JWT Access/Refresh Rotations + Google OAuth2 SSO
- **Deployment**: Docker, Docker-Compose, and Railway

---

## 📂 Project Module Structure

```text
backend/
├── prisma/
│   └── schema.prisma         # Multi-tenant PostgreSQL database models
├── src/
│   ├── main.ts               # Core app bootstrapper, CORS, Global pipes & filters
│   ├── app.module.ts         # Global app module tying all sub-systems
│   ├── auth/                 # Local & Google OAuth2 Authentication strategies & guards
│   ├── users/                # User lifecycle, RBAC controls (Admin, Technician, User)
│   ├── devices/              # Device provisioning, sharing, and telemetry logs
│   ├── mqtt/                 # EMQX Client connection & subscription engine
│   ├── automations/          # Real-time condition-action automation engine
│   ├── notifications/        # User-specific push notification caches (Redis-backed)
│   ├── ota/                  # Firmware catalog & semantic version distribution
│   ├── ai/                   # Context-aware AI Assistant powered by Google Gemini
│   └── common/               # NestJS global filters, interceptors, and decorators
├── Dockerfile                # Production multi-stage docker compiler
├── docker-compose.yml        # Multi-service stack (PostgreSQL, Redis, EMQX, App)
└── railway.json              # Railway CD specifications
```

---

## 📡 MQTT Message Protocol & Topics

The backend integrates with EMQX to capture real-time physical microchip telemetries and publish remote command states.

### 1. Device Telemetry Stream (Subscribe)
Devices publish sensor updates on:
`craftiot/devices/{deviceId}/telemetry`

**Payload Frame:**
```json
{
  "sensorValue1": 24.5,
  "sensorValue2": 62.0,
  "stateFlag1": true
}
```

### 2. Device Remote Commands (Publish)
Remote command dispatches are published on:
`craftiot/devices/{deviceId}/control`

**Payload Frame:**
```json
{
  "deviceId": "esp32_climate_01",
  "stateFlag1": false,
  "timestamp": "2026-07-04T17:10:00.000Z"
}
```

---

## 🚀 Getting Started (Local Development)

### 1. Stand up services with Docker Compose
```bash
docker-compose up --build -d
```
This spawns:
- **PostgreSQL**: `localhost:5432`
- **Redis**: `localhost:6379`
- **EMQX Broker**: `localhost:1883` (Dashboard: `http://localhost:18083`)
- **NestJS App**: `http://localhost:3000`

### 2. Prepare Environment Variables
Copy `.env.example` to `.env` and fill in:
- `JWT_ACCESS_SECRET`
- `JWT_REFRESH_SECRET`
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`
- `GEMINI_API_KEY`

### 3. Run Database Migrations
```bash
npx prisma migrate dev --name init
```

### 4. Run Tests
```bash
# Unit Tests
npm run test

# End-to-End Tests
npm run test:e2e
```

### 5. View Swagger Documentation
Once the server is booted, access the OpenAPI documentation platform at:
`http://localhost:3000/api/docs`

---

## 🔒 Role-Based Access Control (RBAC)

The system enforces granular authorization levels on endpoints:
- **USER**: Accesses owned devices, shared devices, notifications, and AI Assistant.
- **TECHNICIAN**: Views devices, telemetry histories, and registers OTA firmware updates.
- **ADMIN**: Complete system control, user deletions, role management, and system-wide configurations.
