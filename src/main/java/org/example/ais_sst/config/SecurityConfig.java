package org.example.ais_sst.config;  // ПРАВИЛЬНО!

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

    private final AuthTokenFilter authTokenFilter;  // ДОЛЖНО БЫТЬ FINAL!

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
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080", "*", "http://localhost:8081"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token", "Authorization"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token", "Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("========== НАСТРОЙКА БЕЗОПАСНОСТИ ==========");
        log.info("Configuring security with CSRF DISABLED...");

        if (authTokenFilter == null) {
            log.error("authTokenFilter is NULL!");
            throw new IllegalStateException("authTokenFilter cannot be null");
        }
        log.info("authTokenFilter is properly injected: {}", authTokenFilter.getClass().getName());

        http
                .csrf(csrf -> {
                    log.info("CSRF explicitly DISABLED");
                    csrf.disable();
                })
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // ДОБАВЬТЕ ЭТУ СТРОКУ - обработчик ошибок аутентификации
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(authz -> {
                    authz
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/auth/**")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/test/**")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.POST, "/api/account_requests")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/debug/**")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/social_status")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/api/specialities")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.GET, "/api/group")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/error")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/favicon.ico")).permitAll()
                            .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll()
                            .anyRequest().authenticated();
                    log.info("Authorization rules configured");
                })
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("Security configuration completed");
        log.info("========== НАСТРОЙКА ЗАВЕРШЕНА ==========");
        return http.build();
    }
}