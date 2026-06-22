package org.example.ais_sst.config;

import org.example.ais_sst.interceptor.WebSocketAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Клиент отправляет на сервер по префиксу /app
        config.setApplicationDestinationPrefixes("/app");

        // Встроенный брокер для рассылки
        config.enableSimpleBroker("/topic", "/queue");

        // Для личных уведомлений
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-endpoint")
                .setAllowedOrigins(
                        "https://ais-sst.ru",
                        "https://app.ais-sst.ru"
                ) // Укажите конкретные домены!
                .withSockJS();
    }
}