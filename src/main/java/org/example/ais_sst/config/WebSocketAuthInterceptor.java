package org.example.ais_sst.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.security.jwt.JwtUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        log.info("════════════════════════════════════════════════════════════");
        log.info(">>> [INTERCEPTOR] preSend вызван");
        log.info(">>> Command: {}", accessor != null ? accessor.getCommand() : "null");
        log.info(">>> SessionId: {}", accessor != null ? accessor.getSessionId() : "null");

        if (accessor == null) {
            log.warn(">>> accessor == null — пропускаем");
            return message;
        }

        // Логируем все заголовки
        log.info(">>> Native Headers: {}", accessor.toNativeHeaderMap());

        // Пытаемся залогировать сырое тело сообщения
        Object payload = message.getPayload();
        if (payload instanceof byte[]) {
            String raw = new String((byte[]) payload);
            log.info(">>> Raw payload (первые 500 символов): {}", raw.length() > 500 ? raw.substring(0, 500) + "..." : raw);
        } else {
            log.info(">>> Payload type: {}", payload.getClass().getSimpleName());
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info(">>> === ОБРАБОТКА CONNECT ===");

            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            log.info(">>> Authorization headers: {}", authHeaders);

            if (authHeaders == null || authHeaders.isEmpty()) {
                log.warn(">>> ОТКЛОНЕНО: Нет заголовка Authorization");
                return null;
            }

            try {
                String rawHeader = authHeaders.get(0);
                log.info(">>> Raw Authorization header: {}", rawHeader);

                String token = rawHeader.replace("Bearer ", "").trim();
                log.info(">>> Token length: {}", token.length());

                String username = jwtUtils.getUserNameFromJwtToken(token);
                log.info(">>> Username из токена: {}", username);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                accessor.setUser(authentication);
                log.info(">>> УСПЕШНАЯ АУТЕНТИФИКАЦИЯ: {}", username);

            } catch (Exception e) {
                log.error(">>> ОШИБКА АУТЕНТИФИКАЦИИ: {}", e.getMessage(), e);
                return null;
            }
        }

        log.info(">>> Возвращаем сообщение дальше");
        log.info("════════════════════════════════════════════════════════════");
        return message;
    }
}