package org.rooms.roombay.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.StringUtils;

/**
 * Copies {@code signup_role} from the query string into {@link OAuth2AuthorizationRequest}
 * attributes so it is stored with Spring's saved authorization request (same session entry as
 * OAuth {@code state}). Session-only filters on the first hop are unreliable behind some proxies.
 */
public class SignupRoleAwareOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    static final String ATTR_SIGNUP_ROLE = "roombay_signup_role";

    private final OAuth2AuthorizationRequestResolver delegate;

    public SignupRoleAwareOAuth2AuthorizationRequestResolver(OAuth2AuthorizationRequestResolver delegate) {
        this.delegate = delegate;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return enrich(request, delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, @NonNull String clientRegistrationId) {
        return enrich(request, delegate.resolve(request, clientRegistrationId));
    }

    private static OAuth2AuthorizationRequest enrich(HttpServletRequest request, OAuth2AuthorizationRequest resolved) {
        if (resolved == null) {
            return null;
        }
        String raw = request.getParameter("signup_role");
        if (!StringUtils.hasText(raw)) {
            return resolved;
        }
        String normalized = raw.trim().toUpperCase();
        String stored;
        if ("LANDLORD".equals(normalized)) {
            stored = "LANDLORD";
        } else if ("STUDENT".equals(normalized) || "TENANT".equals(normalized)) {
            stored = "STUDENT";
        } else {
            return resolved;
        }
        return OAuth2AuthorizationRequest.from(resolved)
                .attributes(attrs -> attrs.put(ATTR_SIGNUP_ROLE, stored))
                .build();
    }
}
