package com.brainway.stomp.demo.controller;

import com.brainway.stomp.demo.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/mqtt")
public class MqttAuthController {

    private static final Logger logger = LoggerFactory.getLogger(MqttAuthController.class);
    private final JwtService jwtService;

    public MqttAuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/user")
    public ResponseEntity<String> authenticateUser(@RequestParam Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        logger.info("Auth Request: User Check. Params: {}", params);

        if (username == null || password == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("deny");
        }

        // Password is the JWT token
        if (jwtService.validateToken(password)) {
            logger.info("Auth Success: User {}", username);
            return ResponseEntity.ok("allow");
        } else {
            logger.warn("Auth Failed: User {}", username);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("deny");
        }
    }

    @PostMapping("/vhost")
    public ResponseEntity<String> authorizeVhost(@RequestParam Map<String, String> params) {
        logger.info("Auth Request: VHost Check. Params: {}", params);
        return ResponseEntity.ok("allow");
    }

    @PostMapping("/resource")
    public ResponseEntity<String> authorizeResource(@RequestParam Map<String, String> params) {
        logger.info("Auth Request: Resource Check. Params: {}", params);
        // Phase 1: Allow all resources
        return ResponseEntity.ok("allow");
    }

    @PostMapping("/topic")
    public ResponseEntity<String> authorizeTopic(@RequestParam Map<String, String> params) {
        logger.info("Auth Request: Topic Check. Params: {}", params);
        params.forEach((k, v) -> logger.info("Param: {} = {}", k, v));

        String username = params.get("username");
        String topic = params.get("routing_key"); // RabbitMQ passes topic as 'routing_key'
        String permission = params.get("permission"); // read (subscribe) or write (publish)

        if (username == null || topic == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("deny");
        }

        // Mock User-Org Mapping
        String allowedOrg = "unknown";
        if ("vasya".equals(username))
            allowedOrg = "org1";
        else if ("petya".equals(username))
            allowedOrg = "org2";
        else if ("admin".equals(username))
            return ResponseEntity.ok("allow"); // Admin access

        // Topic Structure: event/{org}/{type}/{id}
        // or wildcard: event/{org}/#

        // Topic Structure: event.<org>.<type>.<id> (from RabbitMQ) or
        // event/<org>/<type>/<id> (MQTT)
        // RabbitMQ passes the underlying routing key which might be dot-separated for
        // 'amq.topic'
        String[] parts = topic.split("[/.]");

        // Basic check: Must start with "event" (or whatever root you want)
        if (parts.length > 1 && "event".equals(parts[0])) {
            String topicOrg = parts[1];

            if (allowedOrg.equals(topicOrg)) {
                logger.info("Topic Auth [ALLOWED]: User {} allowed for org {}", username, topicOrg);
                return ResponseEntity.ok("allow");
            } else {
                logger.warn("Topic Auth [DENIED]: User {} (org={}) tried to access org {}", username, allowedOrg,
                        topicOrg);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("deny");
            }
        }

        // Default Deny for unknown structures
        logger.warn("Topic Auth [DENIED]: Unknown topic structure: {}", topic);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("deny");
    }
}
