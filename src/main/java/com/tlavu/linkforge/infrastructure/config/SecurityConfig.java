package com.tlavu.linkforge.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import com.tlavu.linkforge.infrastructure.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource, JwtAuthenticationFilter jwtAuthFilter) {
        this.corsConfigurationSource = corsConfigurationSource;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/links/**").permitAll()
                        .requestMatchers("/r/**").permitAll() // Short link resolution
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Webhook from VNPay (No JWT token attached)
                        .requestMatchers("/api/v1/payments/vnpay-return").permitAll()

                        // Admin endpoints
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")

                        // Any other request must be authenticated
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
