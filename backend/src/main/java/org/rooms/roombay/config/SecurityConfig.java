package org.rooms.roombay.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.rooms.roombay.security.JwtAuthenticationFilter;
import org.rooms.roombay.security.OAuthSignupRoleCaptureFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuthSignupRoleCaptureFilter oauthSignupRoleCaptureFilter;
    private final RoomBayGoogleOAuth2SuccessHandler oauth2SuccessHandler;
    private final SignupRoleStashingAuthorizationRequestRepository signupRoleAuthorizationRequestRepository;
    private final OAuth2AuthorizationRequestResolver signupRoleAwareOAuth2AuthorizationRequestResolver;

    @Value("${cors.allowed-origins:}")
    private String corsAllowedOrigins;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            OAuthSignupRoleCaptureFilter oauthSignupRoleCaptureFilter,
            @Autowired(required = false) RoomBayGoogleOAuth2SuccessHandler oauth2SuccessHandler,
            @Autowired(required = false)
                    SignupRoleStashingAuthorizationRequestRepository signupRoleAuthorizationRequestRepository,
            @Autowired(required = false) OAuth2AuthorizationRequestResolver signupRoleAwareOAuth2AuthorizationRequestResolver) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.oauthSignupRoleCaptureFilter = oauthSignupRoleCaptureFilter;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.signupRoleAuthorizationRequestRepository = signupRoleAuthorizationRequestRepository;
        this.signupRoleAwareOAuth2AuthorizationRequestResolver = signupRoleAwareOAuth2AuthorizationRequestResolver;
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (oauth2SuccessHandler != null) {
            http.oauth2Login(o -> {
                o.successHandler(oauth2SuccessHandler);
                if (signupRoleAuthorizationRequestRepository != null
                        && signupRoleAwareOAuth2AuthorizationRequestResolver != null) {
                    o.authorizationEndpoint(a -> a
                            .authorizationRequestRepository(signupRoleAuthorizationRequestRepository)
                            .authorizationRequestResolver(signupRoleAwareOAuth2AuthorizationRequestResolver));
                }
            });
        }
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31_536_000))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'")))
                // OAuth2 authorization-code flow needs a session during the redirect; API calls stay JWT-based.
                .sessionManagement(session -> session.sessionCreationPolicy(
                        oauth2SuccessHandler != null
                                ? SessionCreationPolicy.IF_REQUIRED
                                : SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                        auth.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
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
                        ).permitAll();
                        if (!isProdProfile()) {
                            auth.requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();
                        }
                        // PUBLIC: Allow browsing listings without login (critical for growth)
                        auth.requestMatchers("GET", "/api/search").permitAll()
                        .requestMatchers("GET", "/api/feed").permitAll()
                        .requestMatchers("GET", "/api/listings", "/api/listings/search", "/api/listings/active", "/api/listings/featured").permitAll()
                        .requestMatchers("GET", "/api/listings/*/similar").permitAll()
                        .requestMatchers("GET", "/api/listings/*/ar-markers").permitAll()
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
                        .requestMatchers("/api/ai/ingest-dev", "/api/ai/graph-stats-dev").permitAll()
                        // Internal AI tool endpoints for the LangGraph orchestrator sidecar.
                        // Not JWT-gated: authenticated by the shared internal token inside the controller.
                        .requestMatchers("/internal/ai/**").permitAll()
                        // Admin only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Must run before Spring redirects to Google, otherwise signup_role never reaches the session.
        if (oauth2SuccessHandler != null) {
            http.addFilterBefore(oauthSignupRoleCaptureFilter, OAuth2AuthorizationRequestRedirectFilter.class);
        } else {
            http.addFilterBefore(oauthSignupRoleCaptureFilter, JwtAuthenticationFilter.class);
        }

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
            origins.addAll(Arrays.stream(corsAllowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isBlank())
                    .toList());
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

    private boolean isProdProfile() {
        return activeProfiles != null && Arrays.stream(activeProfiles.split(","))
                .map(String::trim)
                .anyMatch("prod"::equalsIgnoreCase);
    }
    
}

