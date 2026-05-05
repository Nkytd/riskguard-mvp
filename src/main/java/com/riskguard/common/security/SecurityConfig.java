package com.riskguard.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskguard.common.api.ApiResponse;
import com.riskguard.common.api.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) ->
                                writeSecurityResponse(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeSecurityResponse(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Forbidden")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(
                                "/doc.html",
                                "/webjars/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/risk/decisions/simulate").hasAnyRole("ADMIN", "RISK_OPERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/risk/decisions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/risk/decisions").hasAnyRole("ADMIN", "RISK_OPERATOR", "AUDITOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/risk/decisions/**").hasAnyRole("ADMIN", "RISK_OPERATOR", "AUDITOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/profiles/**").hasAnyRole("ADMIN", "RISK_OPERATOR", "AUDITOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/cases").hasAnyRole("ADMIN", "RISK_OPERATOR", "AUDITOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/cases/**").hasAnyRole("ADMIN", "RISK_OPERATOR", "AUDITOR")
                        .requestMatchers(HttpMethod.POST, "/api/v1/cases/**").hasAnyRole("ADMIN", "RISK_OPERATOR")
                        .requestMatchers("/api/v1/rules", "/api/v1/rules/**").hasAnyRole("ADMIN", "RISK_OPERATOR")
                        .requestMatchers("/api/v1/strategies", "/api/v1/strategies/**").hasAnyRole("ADMIN", "RISK_OPERATOR")
                        .requestMatchers("/api/v1/risk-lists", "/api/v1/risk-lists/**").hasAnyRole("ADMIN", "RISK_OPERATOR")
                        .requestMatchers("/api/v1/dashboard", "/api/v1/dashboard/**").hasAnyRole("ADMIN", "RISK_OPERATOR", "AUDITOR")
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    private void writeSecurityResponse(HttpServletResponse response, HttpStatus status,
                                       ErrorCode errorCode, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(errorCode.code(), message));
    }
}
