package org.rooms.roombay.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

/**
 * Google OAuth client registration. Requires {@code GOOGLE_CLIENT_ID} and {@code GOOGLE_CLIENT_SECRET}
 * when {@code app.oauth.google.enabled=true}.
 * <p>
 * Uses Spring Security's default redirect template so the redirect URI matches the actual request URL
 * (after {@code server.forward-headers-strategy} when behind Next.js/ngrok). Register every base URL you use
 * in Google Cloud Console, e.g. {@code http://localhost:8082/login/oauth2/code/google} and your public HTTPS
 * backend base (ngrok) with the same path suffix.
 */
@Configuration
@ConditionalOnProperty(name = "app.oauth.google.enabled", havingValue = "true")
public class GoogleOAuthClientConfig {

    private static final String GOOGLE_REDIRECT_TEMPLATE =
            "{baseUrl}/login/oauth2/code/{registrationId}";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${GOOGLE_CLIENT_ID:}") String clientId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String clientSecret) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new IllegalStateException(
                    "GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are required when app.oauth.google.enabled=true");
        }
        ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .redirectUri(GOOGLE_REDIRECT_TEMPLATE)
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }
}
