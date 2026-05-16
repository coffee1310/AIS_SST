package org.example.ais_sst.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, java.io.IOException {

        log.error("Unauthorized error: {}", authException.getMessage());

        Exception jwtException = (Exception) request.getAttribute("jwtException");

        String errorMessage = "Authentication required";
        String errorType = "Unauthorized";
        int status = HttpStatus.UNAUTHORIZED.value();

        if (jwtException instanceof io.jsonwebtoken.ExpiredJwtException) {
            errorMessage = "Token has expired. Please login again.";
            errorType = "Token Expired";
            log.warn("JWT token expired for request: {}", request.getRequestURI());
        } else if (jwtException instanceof io.jsonwebtoken.MalformedJwtException) {
            errorMessage = "Invalid token format.";
            errorType = "Invalid Token";
        } else if (jwtException instanceof io.jsonwebtoken.security.SignatureException) {
            errorMessage = "Invalid token signature.";
            errorType = "Invalid Signature";
        } else if (jwtException instanceof IllegalArgumentException && jwtException.getMessage().contains("Token not found")) {
            errorMessage = "No token provided.";
            errorType = "Missing Token";
        }

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status);
        body.put("error", errorType);
        body.put("message", errorMessage);
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}