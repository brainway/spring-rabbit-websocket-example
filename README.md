# Spring Boot + RabbitMQ "Shared Nothing" Architecture PoC

This project demonstrates a highly resilient, active-active WebSocket architecture where users are guaranteed **Zero Message Loss** even in the face of network failures, server crashes, or browser disconnects.

## 🚀 How to Run

### 1. Prerequisites
- **Java 21+**
- **Docker** (to run RabbitMQ)

### 2. Start Infrastructure (RabbitMQ + NGINX LB)
We use Docker Compose to start RabbitMQ (Broker) and NGINX (Load Balancer).

```bash
docker-compose up -d
```

- **Port 80**: NGINX Load Balancer (Access App Here)
- **Port 61613**: STOMP Relay
- **Port 15672**: RabbitMQ Management

### 3. Start Application Instances
Run multiple instances on your host machine. NGINX is configured to load balance ports `8080` through `8089`.

**Instance 1:**
```bash
./mvnw spring-boot:run
```

**Instance 2:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

*(You can run up to Instance 10 on port 8089)*

### 4. Connect Client
**IMPORTANT**: Connect via the Load Balancer (Port 80), NOT the direct app ports.

- **[http://localhost](http://localhost)** 

This ensures that if one backend server dies, NGINX will route your reconnection request to the next available server.


---

## 🛡️ Robustness & Guaranteed Delivery Architecture

We treat every message as a critical transaction. Our pipeline ensures that once a message is generated, it is never lost until the client successfully processes it.

### 1. "Shared Nothing" Architecture
- **No Clustering Required**: The Spring Boot apps do not communicate with each other. They do not share memory or session state.
- **Scalability**: You can add 100 server instances. They all simply dump messages into the RabbitMQ Broker Relay.
- **Failover**: If Server A crashes, Server B continues to serve traffic.

### 2. Reliable Publishing (Server -> Broker)
In `ServerEventProducer.java`, we attach the **`persistent: true`** header to every STOMP frame.
```java
headers.put("persistent", "true");
```
- **Why?** When RabbitMQ receives this, it immediately writes the message to its internal disk (Write-Ahead Log).
- **Benefit**: Even if the RabbitMQ server restarts immediately after receiving the message, the data is safe on disk.

### 3. Server-Side Governance (Resource Protection)
In `WebSocketConfig.java`, we use a `ChannelInterceptor` to **enforce** policy on all clients.
```java
accessor.setNativeHeader("x-expires", "3600000"); // 1 Hour
```
- **Why?** Clients cannot be trusted to clean up after themselves.
- **Benefit**: If a client disconnects forever, their queue (and backed-up messages) will auto-delete after 1 hour. This prevents "Zombie Queues" from crashing the broker with Out-Of-Memory errors.

### 4. Durable Queues (Broker -> Storage)
The Client (`index.html`) subscribes using a **Unique, Persistent Client ID**:
```javascript
'x-queue-name': 'queue-' + myClientId,
'durable': true,
'auto-delete': false
```
- **Why?** Standard STOMP queues are temporary. If the WebSocket drops, the queue vanishes.
- **Benefit**: We create a **Named Permanent Queue** for each user. If you close your laptop, RabbitMQ holds your messages in your specific queue. When you return, they are waiting for you.

### 5. Explicit Client Acknowledgement (Broker -> Client)
We switched the subscription mode to **`ack: 'client'`**:
```javascript
'ack': 'client'
...
processEvent(event);
message.ack(); // <--- Only called AFTER processing
```
- **Why?** Default `auto-ack` deletes the message as soon as it leaves the broker. If the browser crashes while rendering, the message is lost.
- **Benefit**: RabbitMQ "locks" the message but does NOT delete it until the client manually sends `ACK`. If the client crashes processing it, the message stays in the queue and is redelivered immediately upon reconnection.

---

## 🧪 How to Verify Resiliency (The "Pull the Plug" Test)

1.  **Open the App**: Connect to `http://localhost:8080`. See the events flowing in columns.
2.  **Kill the Client**: Close the browser tab entirely (or disconnect WiFi).
3.  **Wait**: Wait 15-30 seconds. The servers are still publishing messages 20, 21, 22...
4.  **Reconnect**: Open `http://localhost:8080` again.
5.  **Verify**: You will see the missing messages (20, 21, 22...) appear **immediately** at the top of the columns. They were safely stored in your Durable Queue during your downtime.
