package org.rooms.roombay.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

/**
 * Binds signup role into the OAuth2 authorization request and stashes it for the success handler.
 */
@Configuration
@ConditionalOnProperty(name = "app.oauth.google.enabled", havingValue = "true")
public class RoomBayOAuth2LoginExtrasConfig {

    @Bean
    public SignupRoleStashingAuthorizationRequestRepository signupRoleStashingAuthorizationRequestRepository() {
        return new SignupRoleStashingAuthorizationRequestRepository();
    }

    @Bean
    public OAuth2AuthorizationRequestResolver signupRoleAwareOAuth2AuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        return new SignupRoleAwareOAuth2AuthorizationRequestResolver(delegate);
    }
}
