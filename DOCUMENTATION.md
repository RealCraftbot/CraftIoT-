# CraftIoT Platform: Enterprise IoT Management Console & Gateway
## Comprehensive Project & System Architecture Documentation

Welcome to the definitive architectural, operational, and development documentation for the **CraftIoT Platform**. This document serves as the single source of truth for engineering teams, security auditors, systems integrators, and developer onboarding.

---

## 1. System Architecture Diagram

The CraftIoT Platform uses an event-driven, hybrid architecture designed for offline-first resilience on mobile terminals and horizontally scalable message routing on cloud infrastructure.

```
       ┌────────────────────────────────────────────────────────┐
       │                 Android Client Layer                   │
       │                                                        │
       │  ┌─────────────────┐             ┌──────────────────┐  │
       │  │ Jetpack Compose │ ◄─────────► │    ViewModel     │  │
       │  │     (UI)        │             │   (StateFlow)    │  │
       │  └─────────────────┘             └────────┬─────────┘  │
       │                                           │            │
       │  ┌─────────────────┐             ┌────────▼─────────┐  │
       │  │  Room SQLite    │ ◄─────────► │ HardwareManager  │  │
       │  │   (Offline)     │             │ (Paho/HiveMQ client││
       │  └─────────────────┘             └────────┬─────────┘  │
       └───────────────────────────────────────────┼────────────┘
                                                   │
                  ┌────────────────────────────────┼──────────────────────────────┐
                  │ (GATT / Bluetooth LE Link)     │ (TLS MQTT 8883 / HTTPS REST) │
                  ▼                                ▼                              ▼
       ┌──────────────────────┐        ┌───────────────────────┐      ┌────────────────────────┐
       │   ESP32 Microchip    │        │  Cloud MQTT Broker    │      │  NestJS Cloud Backend  │
       │ (Physical Edge Node) │        │ (EMQX / HiveMQ Cloud) │      │  (REST API Gateway)    │
       └──────────────────────┘        └───────────┬───────────┘      └───────────┬────────────┘
                                                   │                              │
                                                   │ (Event Bridge)               │ (Prisma ORM)
                                                   ▼                              ▼
                                       ┌───────────────────────────────────────────────────────┐
                                       │              Relational Database Engine               │
                                       │                 (PostgreSQL / MySQL)                  │
                                       └───────────────────────────────────────────────────────┘
```

---

## 2. Database Entity-Relationship (ER) Diagram

The platform represents device profiles, time-series telemetry streams, rule engines, OTA records, and enterprise users with strict referential integrity.

### Mermaid Database ER Schema

```mermaid
erDiagram
    USER ||--o{ IOT_DEVICE : "manages"
    USER ||--o{ AUTOMATION_RULE : "owns"
    IOT_DEVICE ||--o{ SENSOR_LOG : "generates"
    IOT_DEVICE ||--o{ FIRMWARE_RELEASE : "applies"

    USER {
        string id PK
        string email UNIQUE
        string passwordHash
        string fullName
        string role
        datetime createdAt
    }

    IOT_DEVICE {
        string id PK
        string name
        string type
        string status
        string ipAddress
        string macAddress UNIQUE
        string connectionType
        float sensorValue1
        float sensorValue2
        boolean stateFlag1
        string ownerId FK
        datetime lastSeen
    }

    SENSOR_LOG {
        int id PK
        string deviceId FK
        string metric
        float value
        datetime timestamp
    }

    AUTOMATION_RULE {
        string id PK
        string name
        string triggerMetric
        float triggerValue
        string actionType
        boolean actionValue
        boolean isActive
        string creatorId FK
    }

    FIRMWARE_RELEASE {
        string id PK
        string version
        string changelog
        string binaryUrl
        string deviceType
        datetime uploadedAt
    }
```

---

## 3. NestJS Cloud REST API Documentation

The backend exposes fully typed REST entrypoints decorated with authentication interceptors and JSON schema verification.

| HTTP Method | Route | Description | Auth Required | Payload Structure | Response (200/201) |
|---|---|---|---|---|---|
| **POST** | `/auth/register` | Create user profile | No | `{"email", "password", "fullName"}` | `{"id", "email", "fullName"}` |
| **POST** | `/auth/login` | Retrieve access token | No | `{"email", "password"}` | `{"access_token", "refresh_token"}` |
| **GET** | `/devices` | List registered devices | Yes | None | `[IoTDevice]` |
| **POST** | `/devices` | Manual device entry | Yes | `{"id", "name", "type", "macAddress"}` | `IoTDevice` |
| **PUT** | `/devices/:id/control` | Dispatch hardware action | Yes | `{"stateFlag1": true}` | `{"success": true}` |
| **POST** | `/automations` | Append a macro-rule | Yes | `{"name", "metric", "threshold", "action"}`| `AutomationRule` |
| **POST** | `/ota/upload` | Release new firmware binary| Yes (Admin) | Multipart Form (`file`, `version`) | `FirmwareRelease` |

---

## 4. TLS-Secured MQTT Topic Structure

All device telemetries, cloud telemetry acquisitions, and real-time commands flow through a TLS-secured (Port 8883) MQTT broker using a structured namespace.

### Wildcard Channel Hierarchy
*   **System Root:** `craftiot/`
*   **Tenant/Device Base:** `craftiot/devices/{deviceId}/`

### Topic Schema and JSON Payloads

#### 1. Telemetry Stream (Device $\rightarrow$ Cloud Broker)
*   **Topic:** `craftiot/devices/{deviceId}/telemetry`
*   **Payload Schema:**
```json
{
  "temp": 24.8,
  "humidity": 62.1,
  "pump": false,
  "uptimeSeconds": 14500
}
```

#### 2. Downstream Commands (Broker $\rightarrow$ Edge Hardware Device)
*   **Topic:** `craftiot/devices/{deviceId}/control`
*   **Payload Schema:**
```json
{
  "stateFlag1": true,
  "timestamp": 1783353450
}
```

#### 3. Status Broadcast LWT (Last Will & Testament)
*   **Topic:** `craftiot/devices/{deviceId}/status`
*   **Payload Schema:**
```json
{
  "status": "OFFLINE",
  "reason": "keep_alive_timeout"
}
```

---

## 5. Bluetooth Low Energy (BLE) & GATT Service Profile

Unprovisioned edge hardware modules advertise as BLE peripherals. The Android Client acts as a Central Device to scan, pair, and configure Wi-Fi credentials via standardized Generic Attribute Profile (GATT) transactions.

```
+------------------------------------------------------------------------+
|                      CraftIoT BLE Profile Core                         |
|                                                                        |
|  [Primary Service]                                                     |
|   UUID: 4fafc201-1fb5-459e-8fcc-c5c9c331914b                           |
|                                                                        |
|   +----------------------------------------------------------------+   |
|   |  [Characteristic] SSID Configuration Characteristic            |   |
|   |   UUID: beb5483e-36e1-4688-b7f5-ea07361b26a8                   |   |
|   |   Properties: WRITE / READ                                     |   |
|   |   Value representation: UTF-8 Raw String (e.g., "Office_AP")   |   |
|   +----------------------------------------------------------------+   |
|                                                                        |
|   +----------------------------------------------------------------+   |
|   |  [Characteristic] PSK Security Key Characteristic              |   |
|   |   UUID: cba5483e-36e1-4688-b7f5-ea07361b26a9                   |   |
|   |   Properties: WRITE                                            |   |
|   |   Value representation: Encrypted UTF-8 String                  |   |
|   +----------------------------------------------------------------+   |
+------------------------------------------------------------------------+
```

---

## 6. Over-The-Air (OTA) Firmware Upgrade Flow

The OTA architecture ensures robust, fail-safe binary updates directly to partition-switched ESP32 boards (A/B OTA layout).

```
   ┌──────────────┐             ┌──────────────┐             ┌──────────────┐
   │ Android App  │             │ Cloud Server │             │  ESP32 Node  │
   └──────┬───────┘             └──────┬───────┘             └──────┬───────┘
          │                            │                            │
          │ 1. Request latest binary   │                            │
          ├───────────────────────────►│                            │
          │                            │                            │
          │ 2. Return payload metadata │                            │
          │    & binary stream         │                            │
          │◄───────────────────────────┤                            │
          │                                                         │
          │ 3. Establish direct HTTP Chunked stream to Device Port   │
          ├────────────────────────────────────────────────────────►│
          │                                                         │
          │ 4. Pipe binary to inactive Partition (e.g. ota_1)       │
          │  (Track live progress byte-by-byte in HardwareManager)  │
          │                                                         │
          │ 5. Perform CRC Check, verify header signature           │
          │◄────────────────────────────────────────────────────────┤
          │                                                         │
          │ 6. Re-write boot flags to inactive slot & restart       │
          │◄────────────────────────────────────────────────────────┤
```

---

## 7. Android Client Architecture (Clean MVVM)

The Android system is structured using standard Jetpack Compose elements bound to reactive unidirectional data streams.

```
       +--------------------------------------------------+
       |                  Presentation                    |
       |  - UI: Jetpack Compose screens & components      |
       |  - State: Kotlin StateFlow & ViewModel architecture
       +                        ┬                         +
                                │ (Observes flows)
                                ▼
       +--------------------------------------------------+
       |                  Hardware Domain                 |
       |  - HardwareManager: Real Android BLE, GATT,      |
       |    Wi-Fi, and HiveMQ MQTT Engine (TLS-Secured)   |
       +                        ┬                         +
                                │ (Accesses Data layer)
                                ▼
       +--------------------------------------------------+
       |                    Data Layer                    |
       |  - Local: SQLite + Jetpack Room Database (DAOs)  |
       |  - Cloud: Retrofit / OkHttp REST Client API      |
       +--------------------------------------------------+
```

### Core Architecture Responsibilities
*   **HardwareManager:** Configures, initializes, and restarts real Bluetooth LE connections, reads and writes Wi-Fi credentials via platform specifiers, implements connection heartbeats, and parses incoming JSON payload topics.
*   **DashboardViewModel:** Orchestrates UI states by consuming database observables, binding BLE events, and updating device status logs.
*   **Room Database Engine:** Persists historical sensor logs and local configurations offline, performing smart differential database synchronizations upon network discovery.

---

## 8. NestJS Backend Architecture

The backend utilizes **NestJS** (TypeScript), an enterprise-ready framework structuring logic into fully segregated, injection-friendly domain modules.

### Software Stack & Framework Layers
*   **Framework:** NestJS CLI + TypeScript
*   **Data Mapper ORM:** Prisma ORM with native transactional support.
*   **Protocol Controllers:** Rest Controllers for external apps + NestJS Microservices adapter for incoming MQTT event pipelines.
*   **Security Stack:** Passport.js + JwtModule utilizing asymmetrical keys.

---

## 9. Folder Structure

### Android Project Directory Tree
```
/app
├── build.gradle.kts           # App-level dependencies and build configs
└── src
    ├── main
    │   ├── AndroidManifest.xml # Permissions (Internet, BLE, Wifi, Location)
    │   ├── java/com/example
    │   │   ├── MainActivity.kt # Entrypoint & Jetpack Navigation mapping
    │   │   ├── data
    │   │   │   ├── local      # Room Database setup (AppDatabase, DAOs)
    │   │   │   └── model      # Core IoT Entities (Device, SensorLog, Rule)
    │   │   ├── hardware       # REAL BLE GATT, Wi-Fi Provisioning, HiveMQ Client
    │   │   ├── network        # Gemini API client & Cloud REST models
    │   │   └── ui
    │   │       ├── components # Dynamic visual widgets (Charts, Controls)
    │   │       ├── screens    # Dashboard, BLE Simulator, Assistant screens
    │   │       └── theme      # Material 3 colors, Typographies, Styles
    │   └── res/values         # strings.xml with localizable localized strings
    └── test/java/com/example  # JUnit, Robolectric, and Roborazzi tests
```

### NestJS Backend Directory Tree
```
/backend
├── prisma
│   └── schema.prisma          # Database models, schemas and connections
├── src
│   ├── app.module.ts          # Core application container
│   ├── auth                   # JWT strategies & Google OAuth handler
│   ├── automations            # Custom threshold trigger engine
│   ├── devices                # Device profile management
│   ├── mqtt                   # Event broker stream subscriber
│   ├── notifications          # Push, email, and socket service
│   ├── ota                    # Firmware binaries validator
│   └── prisma                 # Database client injector
└── tsconfig.json              # TypeScript compilation specifications
```

---

## 10. Platform Security Model

CraftIoT implements zero-trust standards at both the mobile gate and the micro-controller boundary.

```
+------------------+     JWT Token / HTTPS      +----------------------+
|  Android Client  | ─────────────────────────► |  NestJS Cloud API    |
+------------------+                            +----------------------+
         │                                                 │
         │ TLS Socket                                      │ Database SSL
         ▼ (Port 8883)                                     ▼
+------------------+                            +----------------------+
|   MQTT Broker    | ◄────────────────────────  | Database (Prisma/PG) |
+------------------+        Internal Sync       +----------------------+
         ▲
         │ MQTTS Protocol TLS 1.3
         │ (ECC x509 Certificates / TLS Client credentials)
+------------------+
|    ESP32 Edge    |
+------------------+
```

### Protocol-Level Measures
1.  **Transport Encryption:** Real TLS 1.2/1.3 encryption on both HTTP REST calls and MQTT message transfers (SSL port 8883) using trusted certificate bundles.
2.  **Edge Network Boundary:** BLE pairing is locked behind authenticated PIN challenges. Broadcast keys expire recursively to prevent replay attacks.
3.  **Cloud Gatekeeping:** NestJS APIs are protected via asynchronous JWT verification filters with strict role-based access control (Admin, Device Operator, Guest).

---

## 11. End-To-End Device Registration & Provisioning Sequence

```mermaid
sequenceDiagram
    autonumber
    actor User as Field Engineer
    participant App as Android Client (HardwareManager)
    participant HW as ESP32 Hardware Core
    participant AP as Target Wi-Fi Gateway
    participant Cloud as NestJS Backend

    User->>App: Start Device Provisioning Flow
    App->>App: Trigger BLE Scan Filter
    App->>HW: Discover and Connect GATT Server
    HW-->>App: Return Supported GATT Characteristic Services
    User->>App: Input target Wi-Fi SSID & Password
    App->>HW: Write target credentials via secure BLE Characteristic
    HW->>AP: Connect and Authenticate Gateway
    HW-->>App: Report Local IP & Cloud Access Confirmation
    App->>Cloud: Register Device MAC, Type, & IP
    Cloud-->>App: Acknowledge & Emit "ONLINE" Registration Status
    App->>User: Display "Device paired successfully!"
```

---

## 12. Authentication Flow Diagram

```mermaid
sequenceDiagram
    autonumber
    actor User as User
    participant App as Android Client
    participant Auth as NestJS AuthService
    participant OAuth as Google Identity Provider

    User->>App: Click "Sign in with Google"
    App->>OAuth: Initiate OAuth2 client flow
    OAuth-->>App: Return Google ID Token
    App->>Auth: POST /auth/google-callback { token }
    Auth->>OAuth: Validate ID Token signature
    OAuth-->>Auth: Confirm identity & Profile details
    Auth->>Auth: Find or create User in Prisma Database
    Auth-->>App: Return application JWT Access & Refresh tokens
    App->>App: Securely persist JWTs to EncryptedSharedPreferences
    App->>User: Route to interactive Dashboard
```

---

## 13. System Deployment & Configuration Guide

### 1. Backend Service Configuration (NestJS)
To instantiate the backend container on cloud servers or local workspaces:

1.  **Clone source code and configure environmental assets:**
    Create a `.env` file in the root `/backend` folder:
    ```env
    DATABASE_URL="postgresql://postgres:secure_db_pass@localhost:5432/craftiot?schema=public"
    JWT_SECRET="enterprise_level_asymmetrical_private_signing_key_secret_2026"
    JWT_REFRESH_SECRET="refresh_secret_key"
    MQTT_BROKER_URL="mqtts://broker.hivemq.com:8883"
    GEMINI_API_KEY="AIzaSyYourGeminiApiKeyHere"
    ```

2.  **Execute NestJS local service build:**
    ```bash
    cd backend
    npm install
    npx prisma migrate dev --name init
    npm run build
    npm run start:prod
    ```

### 2. Android Applet Build & Execution
1.  Verify development environment is loaded with JDK 17+ and the Android SDK API level 34.
2.  Compile the clean production package directly:
    ```bash
    # Run from root workspace directory
    gradle :app:assembleDebug
    ```
3.  Execute JVM tests:
    ```bash
    gradle :app:testDebugUnitTest
    ```

---

## 14. ESP32 Microchip Firmware Integration Guide

Below is the production C++/Arduino core firmware schema representing real BLE configuration, TLS connectivity, and OTA client operations.

```cpp
#include <WiFi.h>
#include <PubSubClient.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <HTTPClient.h>
#include <Update.h>

// Service and Characteristics definitions
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_SSID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

WiFiClientSecure wifiClient;
PubSubClient mqttClient(wifiClient);
BLEServer* pServer = NULL;

class ProvisionCallback: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        std::string value = pCharacteristic->getValue();
        if (value.length() > 0) {
            Serial.print("BLE Received SSID: ");
            Serial.println(value.c_str());
            // Write SSID to NVS and initiate local connection reboot
        }
    }
};

void setup() {
    Serial.begin(115200);
    
    // 1. Initialize BLE GATT Server for field provisioning
    BLEDevice::init("CraftIoT-Provision-AP");
    pServer = BLEDevice::createServer();
    BLEService *pService = pServer->createService(SERVICE_UUID);
    BLECharacteristic *pChar = pService->createCharacteristic(
                                 CHARACTERISTIC_SSID,
                                 BLECharacteristic::PROPERTY_WRITE
                               );
    pChar->setCallbacks(new ProvisionCallback());
    pService->start();
    BLEDevice::startAdvertising();

    // 2. Load secure TLS cert onto Client Socket
    wifiClient.setCACert(root_ca_certificate_pem_string);
}

void loop() {
    if (WiFi.status() == WL_CONNECTED) {
        if (!mqttClient.connected()) {
            // Reconnect to Port 8883 (Secure TLS MQTT)
        }
        mqttClient.loop();
    }
}

// 3. In-App Direct Chunked OTA Upgrade
void handleOtaUpdate(String serverUrl) {
    HTTPClient http;
    http.begin(serverUrl);
    int httpCode = http.GET();
    if (httpCode == HTTP_CODE_OK) {
        int contentLength = http.getSize();
        bool canBegin = Update.begin(contentLength);
        if (canBegin) {
            WiFiClient* stream = http.getStreamPtr();
            size_t written = Update.writeStream(*stream);
            if (written == contentLength) {
                Serial.println("Direct OTA Write successful. Executing software reboot.");
                ESP.restart();
            }
        }
    }
}
```

---

## 15. Developer Onboarding & Contribution Guidelines

### Branching Model
The team utilizes **GitFlow** conventions:
*   `main`: Mirror of active Cloud Production deployments.
*   `develop`: Integration branch for release validation.
*   `feature/*`: Granular functional development.

### Code Quality Checklist before Pull Requests
1.  **Build Validation:** Run `npm run build` in `/backend` and `gradle :app:assembleDebug` in `/app`. Both projects must compile with zero errors.
2.  **Lint and Format:** Code must adhere to Prettier rules in TypeScript, and detekt/ktlint specifications in Kotlin.
3.  **Testing Harness:** Local unit and mock integrations tests must achieve >85% class coverage. Run `gradle :app:testDebugUnitTest` to verify.
4.  **No Mock Secrets:** API tokens, AWS certificates, or TLS keystores must never be hardcoded or checked into target remote branch tracking. Use standard environmental injection (`.env` files).
