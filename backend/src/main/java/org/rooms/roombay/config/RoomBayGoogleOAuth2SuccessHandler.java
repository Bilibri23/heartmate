package org.rooms.roombay.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.AuthResponse;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.security.OAuthSignupRoleCaptureFilter;
import org.rooms.roombay.service.AuthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * After Google OAuth, issues RoomBay JWTs and redirects to the SPA with tokens in the URL fragment
 * (not sent to the server). The callback page stores tokens and calls {@code GET /api/auth/me}.
 */
@Component
@ConditionalOnProperty(name = "app.oauth.google.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RoomBayGoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    /** Same-origin cookie set by the SPA before redirecting to Google; forwarded to Spring on callback. */
    static final String OAUTH_SIGNUP_ROLE_COOKIE = "roombay_oauth_signup_role";

    private final AuthService authService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String sub = oauthUser.getAttribute("sub");
        String email = oauthUser.getAttribute("email");
        String given = oauthUser.getAttribute("given_name");
        String family = oauthUser.getAttribute("family_name");
        String name = oauthUser.getAttribute("name");
        if ((given == null || given.isBlank()) && name != null && !name.isBlank()) {
            String[] parts = name.trim().split("\\s+", 2);
            given = parts[0];
            family = parts.length > 1 ? parts[1] : "";
        }
        if (given == null || given.isBlank()) {
            given = "User";
        }
        if (family == null) {
            family = "";
        }

        User.UserRole signupHint = readSignupRoleHint(request, response);

        AuthResponse tokens = authService.authenticateWithGoogle(sub, email, given, family, signupHint);
        String fragment = "access_token=" + enc(tokens.getAccessToken())
                + "&refresh_token=" + enc(tokens.getRefreshToken())
                + "&expires_in=" + tokens.getExpiresIn();
        String base = resolveFrontendBaseUrl(request);
        String target = base + "/auth/oauth-callback#" + fragment;
        log.debug("OAuth success redirect to SPA callback (base derived from request when possible)");
        response.sendRedirect(target);
    }

    /**
     * 1) Cookie from SPA (survives Google round-trip on the public host when forwarded to Spring).
     * 2) Session pending from {@link SignupRoleStashingAuthorizationRequestRepository}.
     * 3) Legacy session attribute from {@link OAuthSignupRoleCaptureFilter}.
     */
    private static User.UserRole readSignupRoleHint(HttpServletRequest request, HttpServletResponse response) {
        User.UserRole fromCookie = readSignupRoleFromCookie(request, response);
        if (fromCookie != null) {
            return fromCookie;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        try {
            Object pending = session.getAttribute(SignupRoleStashingAuthorizationRequestRepository.SESSION_PENDING_SIGNUP_ROLE);
            if (pending != null) {
                session.removeAttribute(SignupRoleStashingAuthorizationRequestRepository.SESSION_PENDING_SIGNUP_ROLE);
            }
            if (pending instanceof String ps && !ps.isBlank()) {
                return User.UserRole.valueOf(ps.trim().toUpperCase());
            }
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        Object raw = session.getAttribute(OAuthSignupRoleCaptureFilter.SESSION_ATTR_SIGNUP_ROLE);
        session.removeAttribute(OAuthSignupRoleCaptureFilter.SESSION_ATTR_SIGNUP_ROLE);
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return User.UserRole.valueOf(s.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private static User.UserRole readSignupRoleFromCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (!OAUTH_SIGNUP_ROLE_COOKIE.equals(c.getName()) || !StringUtils.hasText(c.getValue())) {
                continue;
            }
            String v = c.getValue().trim().toUpperCase();
            clearSignupRoleCookie(response);
            try {
                return User.UserRole.valueOf(v);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private static void clearSignupRoleCookie(HttpServletResponse response) {
        Cookie c = new Cookie(OAUTH_SIGNUP_ROLE_COOKIE, "");
        c.setPath("/");
        c.setMaxAge(0);
        response.addCookie(c);
    }

    /**
     * When the OAuth callback hits Spring through the Next.js proxy, {@code X-Forwarded-Host} /
     * {@code X-Forwarded-Proto} / {@code X-Forwarded-Port} describe the browser origin. Use those
     * so a user on {@code localhost:3000} is not bounced to {@code app.frontend-url} (often ngrok).
     * If those headers are absent (direct Spring), fall back to {@code app.frontend-url}.
     */
    private String resolveFrontendBaseUrl(HttpServletRequest request) {
        try {
            String xfHost = firstCsv(request.getHeader("X-Forwarded-Host"));
            if (StringUtils.hasText(xfHost)) {
                String proto = firstCsv(request.getHeader("X-Forwarded-Proto"));
                if (!StringUtils.hasText(proto)) {
                    proto = "http";
                }
                String xfPort = firstCsv(request.getHeader("X-Forwarded-Port"));
                // host:port or bracketed IPv6 — use as-is (do not append X-Forwarded-Port again).
                if (xfHost.startsWith("[") || xfHost.contains(":")) {
                    return trimTrailingSlash(proto + "://" + xfHost);
                }
                if (StringUtils.hasText(xfPort) && !isDefaultPort(proto, xfPort)) {
                    return trimTrailingSlash(proto + "://" + xfHost + ":" + xfPort);
                }
                return trimTrailingSlash(proto + "://" + xfHost);
            }
        } catch (Exception e) {
            log.warn("Could not derive frontend URL from forwarded headers; using app.frontend-url: {}", e.toString());
        }
        return trimTrailingSlash(frontendUrl);
    }

    private static String firstCsv(String v) {
        if (!StringUtils.hasText(v)) {
            return "";
        }
        int comma = v.indexOf(',');
        return (comma == -1 ? v : v.substring(0, comma)).trim();
    }

    private static boolean isDefaultPort(String proto, String port) {
        if (!StringUtils.hasText(port) || !port.chars().allMatch(Character::isDigit)) {
            return true;
        }
        int p = Integer.parseInt(port);
        return ("http".equalsIgnoreCase(proto) && p == 80) || ("https".equalsIgnoreCase(proto) && p == 443);
    }

    private static String trimTrailingSlash(String s) {
        if (s == null || s.isBlank()) {
            return "http://localhost:3000";
        }
        return s.replaceAll("/$", "");
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
