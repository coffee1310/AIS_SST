package org.example.ais_sst.config;

import org.example.ais_sst.security.jwt.AuthTokenFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.security.jwt.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthTokenFilter authTokenFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ ТОЛЬКО ВАШИ ДОМЕНЫ для продакшна
        // ДЛЯ РАЗРАБОТКИ ОСТАВЬТЕ localhost
        if (isProduction()) {
            configuration.setAllowedOriginPatterns(Arrays.asList(
                    "https://ais-sst.ru",
                    "https://app.ais-sst.ru",
                    "https://admin.ais-sst.ru"
            ));
        } else {
            // ⭐ ДЛЯ РАЗРАБОТКИ - только localhost
            configuration.setAllowedOriginPatterns(Arrays.asList(
                    "http://localhost:*",
                    "http://127.0.0.1:*"
            ));
        }

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "authorization", "content-type", "x-auth-token",
                "Authorization", "X-Requested-With", "Accept"
        ));

        configuration.setExposedHeaders(Arrays.asList(
                "x-auth-token", "Authorization"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);



        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        CorsConfiguration wsConfig = new CorsConfiguration();
        wsConfig.setAllowedOriginPatterns(configuration.getAllowedOriginPatterns());
        wsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS"));
        wsConfig.setAllowedHeaders(Arrays.asList("*"));           // ← важно
        wsConfig.setAllowCredentials(true);
        wsConfig.setMaxAge(3600L);

        source.registerCorsConfiguration("/ws-endpoint/**", wsConfig);

        return source;
    }

    private boolean isProduction() {
        // Используйте Spring profiles
        String profile = System.getProperty("spring.profiles.active");
        return "prod".equals(profile) || "production".equals(profile);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("========== НАСТРОЙКА БЕЗОПАСНОСТИ ==========");
        log.info("Configuring security with CSRF DISABLED...");

        http
                .csrf(csrf -> {
                    log.info("CSRF explicitly DISABLED");
                    csrf.disable();
                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(authz -> {
                    authz
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/**")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/test/**")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/account_requests")).permitAll()

                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/account_requests/send-code")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/account_requests/verify-code")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/account_requests/verify-and-create")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/account_requests/resend-code")).permitAll()

                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/social_status")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/specialities")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/group")).permitAll()

                            // OPTIONS для CORS preflight
                            .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.OPTIONS, "/**")).permitAll()

                            // H2 Console (только для разработки)
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll()

                            // WebSocket handshake разрешаем (нужно для SockJS)
                            // Реальная проверка JWT будет в WebSocketAuthInterceptor на этапе CONNECT
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/ws-endpoint/**")).permitAll()

                            .anyRequest().authenticated();
                })
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("Security configuration completed");
        log.info("========== НАСТРОЙКА ЗАВЕРШЕНА ==========");
        return http.build();
    }
}