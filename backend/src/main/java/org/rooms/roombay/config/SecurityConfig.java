package org.rooms.roombay.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.rooms.roombay.security.JwtAuthenticationFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RoomBayGoogleOAuth2SuccessHandler oauth2SuccessHandler;

    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          @Autowired(required = false) RoomBayGoogleOAuth2SuccessHandler oauth2SuccessHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (oauth2SuccessHandler != null) {
            http.oauth2Login(o -> o.successHandler(oauth2SuccessHandler));
        }
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // OAuth2 authorization-code flow needs a session during the redirect; API calls stay JWT-based.
                .sessionManagement(session -> session.sessionCreationPolicy(
                        oauth2SuccessHandler != null
                                ? SessionCreationPolicy.IF_REQUIRED
                                : SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // Public auth endpoints
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/verify-email",
                                "/ws/**"  // WebSocket endpoints
                        ).permitAll()
                        // Swagger/API docs
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // PUBLIC: Allow browsing listings without login (critical for growth)
                        .requestMatchers("GET", "/api/search").permitAll()
                        .requestMatchers("GET", "/api/feed").permitAll()
                        .requestMatchers("GET", "/api/listings", "/api/listings/search", "/api/listings/featured").permitAll()
                        .requestMatchers("GET", "/api/listings/{id}").permitAll()
                        // Authenticated endpoints
                        .requestMatchers("/api/phone-verification/**").authenticated()
                        .requestMatchers("/api/upload/**").authenticated()
                        .requestMatchers("/api/profiles/**").authenticated()
                        .requestMatchers("/api/verifications/**").authenticated()
                        .requestMatchers("/api/preferences/**").authenticated()
                        .requestMatchers("/api/listing-preferences/**").authenticated()
                        .requestMatchers("/api/matches/**").authenticated()
                        .requestMatchers("/api/share-listing/**").authenticated()
                        .requestMatchers("/api/listings/**").authenticated()
                        .requestMatchers("/api/applications/**").authenticated()
                        .requestMatchers("/api/leases/**").authenticated()
                        .requestMatchers("/api/payments/**").authenticated()
                        .requestMatchers("/api/reviews/**").authenticated()
                        .requestMatchers("/api/disputes/**").authenticated()
                        .requestMatchers("/api/support/**").authenticated()
                        // Local RAG ingest when ROOMBAY_AI_INGEST_DEV_KEY is set (controller returns 404 if unset)
                        .requestMatchers("/api/ai/ingest-dev").permitAll()
                        // Admin only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Build allowed origins: always include localhost for dev, add production origins from env
        List<String> origins = new ArrayList<>(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        if (corsAllowedOrigins != null && !corsAllowedOrigins.isBlank()) {
            origins.addAll(Arrays.asList(corsAllowedOrigins.split(",")));
        }
        configuration.setAllowedOriginPatterns(origins);
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/ws/**", configuration);
        return source;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

}

