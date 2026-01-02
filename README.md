# Multi-Tenant Real-Time Event System (MQTT + Spring Boot)

## 📌 Project Overview

This project is a Proof of Concept (PoC) demonstrating a robust **Multi-Tenant Real-Time Event Streaming Architecture**. It enables secure, organization-isolated real-time communication between a backend server and web clients using **MQTT over WebSockets**.

The system is designed to simulate a scenario where multiple organizations (Tenants) share a single infrastructure but must only receive data relevant to their specific organization and entity types. It enforces strict **Authentication** and **Authorization** at the broker level using RabbitMQ's custom Auth Backend interface delegated to a Spring Boot service.

### Key Capabilities
*   **Multi-Tenancy**: Data isolation between organizations (e.g., `org1` cannot see `org2` events).
*   **Hierarchical Topics**: Events are routed via `event.<org>.<entity_type>.<entity_id>`.
*   **Secure Authentication**: JWT-based login and connection establishment.
*   **Fine-Grained Authorization**: RabbitMQ topic authorization checks delegated to the backend, ensuring users can only subscribe to their permitted scopes.
*   **Multi-Device Simulation**: Unique persistent Client IDs allow multiple browser tabs to simulate distinct devices for the same user simultaneously.
*   **Resilience**: Auto-reconnection logic with strict error handling (e.g., immediate stop on Auth Denial).

---

## 🏗 Architecture

The system consists of three main components:

1.  **RabbitMQ Broker (Docker)**
    *   Acts as the central message bus.
    *   Exposes `MQTT over WebSockets` on port `15675`.
    *   Configured with `rabbitmq_auth_backend_http` to delegate security decisions to the Backend.
    *   Uses a short-lived **Auth Cache** (1 minute) to balance performance and security.

2.  **Backend Server (Spring Boot)**
    *   **Event Producer**: Generates simulated high-frequency events for multiple organizations (`org1`, `org2`) and entity types (`gate`, `manifest`).
    *   **Auth Controller**: Provides endpoints (`/auth/user`, `/auth/vhost`, `/auth/resource`, `/auth/topic`) that RabbitMQ calls to validate credentials and access rights.
    *   **Login API**: Issues JWTs (mocked) for frontend clients.

3.  **Frontend Client (Vanilla JS)**
    *   Connects to RabbitMQ via WebSockets.
    *   Dynamically subscribes to topics based on user selection (e.g., `event/org1/#`).
    *   Visualizes real-time data in dynamic columns.
    *   Handles auth errors gracefully by redirecting to login.

---

## 🚀 Getting Started

### Prerequisites
*   **Java 21+**
*   **Docker & Docker Compose**
*   **Maven**

### 1. Start Infrastructure (RabbitMQ)
Start the RabbitMQ broker with the necessary plugins enabled.

```bash
docker-compose up -d
```

> **Note**: This starts RabbitMQ on ports `5672` (AMQP), `15672` (Management UI), and `15675` (MQTT WebSockets).

### 2. Start Backend Server
Run the Spring Boot application.

```bash
mvn spring-boot:run
```
*The server will start on `localhost:8080`.*

### 3. Access the Application
Open your browser and navigate to:
**http://localhost:8080**

---

## 📖 Usage Guide

### Login & Simulation
The system comes with two pre-configured mock users for testing multi-tenancy:

| Username | Organization | Permissions |
| :--- | :--- | :--- |
| **`user1`** | `org1` | Can subscribe to `event/org1/#` |
| **`user2`** | `org2` | Can subscribe to `event/org2/#` |

#### 1. Successful Connection
1.  Enter Username: **`user1`**
2.  Select Organization: **`Organization 1`**
3.  Select Type: **`All Types`**
4.  Click **Login & Connect**.
    *   **Result**: You will see a green "Connected" status. Real-time events for `org1` (Gates and Manifests) will appear in dynamic columns.

#### 2. Multi-Device Test (Tab Isolation)
1.  Open a **New Tab**.
2.  Login again as **`user1`**.
3.  **Result**: Both tabs will receive events simultaneously. Check the console to see unique `Client ID`s being generated per tab (e.g., `user1-web-x8a9s7`).

#### 3. Authorization Failure Test (Cross-Tenant Access)
1.  Refresh the page (Logout).
2.  Enter Username: **`user1`**
3.  Select Organization: **`Organization 2`** (Malicious attempt).
4.  Click **Login & Connect**.
    *   **Result**: The application attempts to subscribe to `event/org2/#`.
    *   RabbitMQ asks the backend: *"Can user1 read org2?"* -> Backend says **DENY**.
    *   Frontend receives an error, disconnects immediately, and shows: **"Subscription Denied: You are not authorized for org2"**.

---

## 🔧 Technical Details

### Topic Structure
*   **Internal (AMQP)**: `event.<org>.<type>.<id>`
*   **MQTT Subscription**: `event/<org>/<type>/<id>`

### Security Flow
1.  **Connection**: RabbitMQ calls `POST /auth/user`. Backend validates `username` & `password` (JWT).
2.  **Subscription**: Client sends `SUBSCRIBE event/org1/#`.
3.  **Authorization**: RabbitMQ calls `POST /auth/topic`.
    *   Param `routing_key` = `event.org1.#` (or `event.org1.gate.x` depending on context).
    *   Backend checks if `user.org == topic.org`.
    *   Returns `allow` or `deny`.

### Configuration Files
*   **`rabbitmq.conf`**: Configures the HTTP Auth Backend and Caching.
*   **`docker-compose.yml`**: Defines the RabbitMQ container and network.
*   **`MqttAuthController.java`**: The core logic for verifying users and topics.
*   **`index.html`**: The client-side logic for connection, subscription, and UI rendering.

---

## 📝 Troubleshooting

*   **"Connection Closed" immediately**: Usually indicates a configuration mismatch in `rabbitmq.conf` or the Backend is not running.
*   **"Subscription Denied"**: You are trying to access an Org that doesn't belong to your user.
*   **Events not appearing**: Check if the Producer is running (logs should show `Broadcasting: ...`).
