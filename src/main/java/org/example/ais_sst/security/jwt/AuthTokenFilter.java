package org.example.ais_sst.security.jwt;

import org.example.ais_sst.exception.TokenIsNotValidException;
import org.example.ais_sst.exception.TokenRefreshException;
import org.example.ais_sst.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.service.tokens.RevokedTokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private final RevokedTokenService revokedTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // ⭐ OPTIONS запросы всегда пропускаем (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            log.debug("OPTIONS request to: {}, skipping authentication", path);
            filterChain.doFilter(request, response);
            return;
        }

        // ⭐ Проверяем публичные эндпоинты
        if (isPublicEndpoint(path, method)) {
            log.debug("Public endpoint: {} {}, skipping authentication", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = parseJwt(request);

            if ("OPTIONS".equalsIgnoreCase(method)) {
                log.debug("OPTIONS request to: {}, skipping", path);
                filterChain.doFilter(request, response);
                return;
            }

            // ⭐ 2. WebSocket эндпоинты - всегда пропускаем
            if (path.startsWith("/ws-endpoint") || path.startsWith("/websocket")) {
                log.debug("WebSocket endpoint: {}, skipping authentication", path);
                filterChain.doFilter(request, response);
                return;
            }

            if (jwt == null) {
                log.debug("No JWT token found for request to: {}", path);
                sendUnauthorizedResponse(response, "Token not found");
                return;
            }

            // Проверяем валидность токена
            if (!jwtUtils.validateJwtToken(jwt)) {
                log.debug("Invalid JWT token for request to: {}", path);
                sendUnauthorizedResponse(response, "Invalid token");
                return;
            }

            // Получаем JTI из токена
            String jti = jwtUtils.getJtiFromToken(jwt);

            // Проверяем, не отозван ли токен
            if (revokedTokenService.isAccessTokenRevoked(jti)) {
                log.warn("Access token has been revoked for request to: {}, JTI: {}", path, jti);
                sendUnauthorizedResponse(response, "Token has been revoked. Please login again.");
                return;
            }

            String username = jwtUtils.getUserNameFromJwtToken(jwt);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Request authenticated successfully for user: {}", username);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("JWT token expired: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Token expired");
        } catch (io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.security.SignatureException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Invalid token");
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            sendUnauthorizedResponse(response, "Authentication failed");
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"error\": \"%s\", \"status\": 401}", message));
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        String headerAuthLower = request.getHeader("authorization");
        if (headerAuthLower != null && headerAuthLower.startsWith("Bearer ")) {
            return headerAuthLower.substring(7);
        }
        return null;
    }

    private boolean isPublicEndpoint(String path, String method) {
        // ⭐ WebSocket эндпоинты - пропускаем ВСЕ
        if (path.startsWith("/ws-endpoint")) {
            return true;
        }
        if (path.startsWith("/websocket")) {
            return true;
        }

        // Публичные API
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        if (path.startsWith("/api/test/")) {
            return true;
        }
        if (path.equals("/api/social_status")) {
            return true;
        }
        if (path.equals("/api/specialities")) {
            return true;
        }
        if (path.equals("/api/group") && "GET".equals(method)) {
            return true;
        }
        if (path.startsWith("/h2-console/")) {
            return true;
        }
        if (path.equals("/error")) {
            return true;
        }
        if (path.equals("/favicon.ico")) {
            return true;
        }
        if (path.equals("/api/account_requests") && "POST".equals(method)) {
            return true;
        }
        if (path.startsWith("/ws-endpoint") || path.startsWith("/websocket")) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/ws-endpoint");
    }
}