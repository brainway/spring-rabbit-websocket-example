package com.brainway.stomp.demo;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@EnableScheduling
public class ServerEventProducer implements ApplicationListener<ContextRefreshedEvent> {

    private final RabbitTemplate rabbitTemplate;
    private final String serverInstanceId;
    private final AtomicLong sequence = new AtomicLong(new java.util.Random().nextInt(1001));

    public ServerEventProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.serverInstanceId = UUID.randomUUID().toString();
        System.out.println("Starting Server Instance: " + serverInstanceId);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Just to match previous listener pattern, though logic is simplified
        System.out.println("App Context Refreshed - Producer Ready");
    }

    private final String[] ORGS = { "org1", "org2" };
    private final String[] TYPES = { "gate", "manifest" };
    private final String[] MANIFESTS = { "manifest-a", "manifest-b", "manifest-c" };
    private final String[] GATES = { "gate-x", "gate-y", "gate-z" };

    @Scheduled(fixedRate = 1000)
    public void broadcastEvent() {
        String org = ORGS[new java.util.Random().nextInt(ORGS.length)];
        String type = TYPES[new java.util.Random().nextInt(TYPES.length)];
        String id;

        if ("gate".equals(type)) {
            id = GATES[new java.util.Random().nextInt(GATES.length)];
        } else {
            id = MANIFESTS[new java.util.Random().nextInt(MANIFESTS.length)];
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("org", org);
        payload.put("type", type);
        payload.put("id", id);
        payload.put("timestamp", Instant.now().toString());
        payload.put("sequence", sequence.incrementAndGet());
        payload.put("data", "Update for " + id);

        System.out.println("Broadcasting: " + payload);

        // Routing Key: event.<org>.<type>.<id>
        // Example: event.org1.gate.gate-x
        String routingKey = "event." + org + "." + type + "." + id;
        rabbitTemplate.convertAndSend("amq.topic", routingKey, payload);
    }
}
