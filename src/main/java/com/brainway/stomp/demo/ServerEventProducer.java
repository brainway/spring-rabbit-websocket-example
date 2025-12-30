package com.brainway.stomp.demo;

import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
@EnableScheduling
public class ServerEventProducer implements ApplicationListener<BrokerAvailabilityEvent> {

    private final SimpMessagingTemplate messagingTemplate;
    private final String serverInstanceId;
    private final AtomicLong sequence = new AtomicLong(0);
    private final AtomicBoolean brokerAvailable = new AtomicBoolean(false);

    public ServerEventProducer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.serverInstanceId = UUID.randomUUID().toString();
        System.out.println("Starting Server Instance: " + serverInstanceId);
    }

    @Override
    public void onApplicationEvent(BrokerAvailabilityEvent event) {
        this.brokerAvailable.set(event.isBrokerAvailable());
        System.out.println("Broker availability changed: " + event.isBrokerAvailable());
    }

    @Scheduled(fixedRate = 3000)
    public void broadcastEvent() {
        if (!brokerAvailable.get()) {
            System.out.println("Broker not active yet, skipping broadcast...");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("serverId", serverInstanceId);
        payload.put("sequence", sequence.incrementAndGet());
        payload.put("timestamp", Instant.now().toString());

        System.out.println("Broadcasting event from " + serverInstanceId + ": " + payload);

        // "persistent": "true" tells RabbitMQ to write this message to disk if the
        // queue is durable
        Map<String, Object> headers = new HashMap<>();
        headers.put("persistent", "true");

        messagingTemplate.convertAndSend("/topic/events", payload, headers);
    }
}
