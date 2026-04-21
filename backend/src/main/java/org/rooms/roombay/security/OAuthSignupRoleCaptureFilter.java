package org.rooms.roombay.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Stores intended account role for Google OAuth sign-up (register flow) in the HTTP session.
 * Query param {@code signup_role} must be one of: {@code LANDLORD}, {@code STUDENT}, {@code TENANT}
 * ({@code TENANT} is stored as {@code STUDENT} to match backend {@link org.rooms.roombay.entity.User.UserRole}).
 * <p>
 * Only applied when starting {@code /oauth2/authorization/{registrationId}}; cleared after use in
 * {@link org.rooms.roombay.config.RoomBayGoogleOAuth2SuccessHandler}.
 */
@Component
public class OAuthSignupRoleCaptureFilter extends OncePerRequestFilter {

    public static final String SESSION_ATTR_SIGNUP_ROLE = "ROOMBAY_OAUTH_SIGNUP_ROLE";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && uri.contains("/oauth2/authorization/")) {
            String raw = request.getParameter("signup_role");
            if (StringUtils.hasText(raw)) {
                String normalized = raw.trim().toUpperCase();
                if ("LANDLORD".equals(normalized)) {
                    request.getSession(true).setAttribute(SESSION_ATTR_SIGNUP_ROLE, "LANDLORD");
                } else if ("STUDENT".equals(normalized) || "TENANT".equals(normalized)) {
                    request.getSession(true).setAttribute(SESSION_ATTR_SIGNUP_ROLE, "STUDENT");
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
