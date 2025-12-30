# Spring Boot WebSocket + RabbitMQ Shared Nothing PoC

This project demonstrates a multi-instance WebSocket architecture where events generated on any server instance are broadcast to all connected clients via an external RabbitMQ STOMP broker relay.

## Prerequisites

- Java 21+
- Docker (for RabbitMQ)

## 1. Start RabbitMQ with STOMP Plugin

You must run RabbitMQ with the `rabbitmq_stomp` plugin enabled. Run the following command:

```bash
docker run -d --rm --name rabbitmq-stomp \
  -p 5672:5672 \
  -p 15672:15672 \
  -p 61613:61613 \
  rabbitmq:3-management \
  /bin/bash -c "rabbitmq-plugins enable --offline rabbitmq_stomp && rabbitmq-server"
```

- Port `61613`: STOMP port (Default)
- Port `15672`: Management UI (Default login: guest/guest)
- Port `5672`: AMQP port

## 2. Run Application Instances

To verify the "Shared Nothing" architecture, run two separate instances on different ports.

### Instance 1 (Port 8080)
Open a terminal and run:
```bash
./mvnw spring-boot:run
```

### Instance 2 (Port 8081)
Open a second terminal and run:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## 3. Verify

1.  Open [http://localhost:8080](http://localhost:8080) in your browser.
2.  Open [http://localhost:8081](http://localhost:8081) in a **different** browser window or tab.
3.  Observe the event stream. You should see events from **both** Server IDs appearing on **both** clients.
    - `Server: <UUID-1>`
    - `Server: <UUID-2>`

This proves that even though Client A is connected to Server A, it receives messages published by Server B via the RabbitMQ topic.
