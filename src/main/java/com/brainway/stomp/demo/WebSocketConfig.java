package com.brainway.stomp.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Use external RabbitMQ STOMP broker relay
        config.enableStompBrokerRelay("/topic")
                .setRelayHost("localhost")
                .setRelayPort(61613)
                .setClientLogin("guest")
                .setClientPasscode("guest")
                .setSystemLogin("guest")
                .setSystemPasscode("guest");

        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-delivery")
                .setAllowedOriginPatterns("*") // Allow all origins for the demo
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(
            org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new org.springframework.messaging.support.ChannelInterceptor() {
            @Override
            public org.springframework.messaging.Message<?> preSend(org.springframework.messaging.Message<?> message,
                    org.springframework.messaging.MessageChannel channel) {
                org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor = org.springframework.messaging.simp.stomp.StompHeaderAccessor
                        .wrap(message);

                if (org.springframework.messaging.simp.stomp.StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    // Enforce Queue Expiration (1 hour)
                    // This creates a dedicated "Governance" layer that overrides client headers
                    accessor.setNativeHeader("x-expires", "60000");
                }

                return org.springframework.messaging.support.MessageBuilder.createMessage(message.getPayload(),
                        accessor.getMessageHeaders());
            }
        });
    }
}
