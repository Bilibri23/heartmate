package org.rooms.roombay.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.StringUtils;

/**
 * When the authorization code callback runs, Spring removes the saved
 * {@link OAuth2AuthorizationRequest} from the session. We copy {@link SignupRoleAwareOAuth2AuthorizationRequestResolver#ATTR_SIGNUP_ROLE}
 * onto a plain session attribute so {@link RoomBayGoogleOAuth2SuccessHandler} can read it on the same request
 * (no dependence on a separate cookie round-trip from the first OAuth hop).
 */
public class SignupRoleStashingAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    /**
     * Session key read by {@link RoomBayGoogleOAuth2SuccessHandler} after OAuth completes.
     */
    public static final String SESSION_PENDING_SIGNUP_ROLE = "ROOMBAY_OAUTH_PENDING_SIGNUP_ROLE";

    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> delegate =
            new HttpSessionOAuth2AuthorizationRequestRepository();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return delegate.loadAuthorizationRequest(request);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        delegate.saveAuthorizationRequest(authorizationRequest, request, response);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest removed = delegate.removeAuthorizationRequest(request, response);
        if (removed != null) {
            Object role = removed.getAttributes().get(SignupRoleAwareOAuth2AuthorizationRequestResolver.ATTR_SIGNUP_ROLE);
            if (role instanceof String s && StringUtils.hasText(s)) {
                request.getSession(true).setAttribute(SESSION_PENDING_SIGNUP_ROLE, s.trim().toUpperCase());
            }
        }
        return removed;
    }
}
