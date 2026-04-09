package org.rooms.roombay.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.AuthResponse;
import org.rooms.roombay.service.AuthService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

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

        AuthResponse tokens = authService.authenticateWithGoogle(sub, email, given, family);
        String fragment = "access_token=" + enc(tokens.getAccessToken())
                + "&refresh_token=" + enc(tokens.getRefreshToken())
                + "&expires_in=" + tokens.getExpiresIn();
        String target = frontendUrl.replaceAll("/$", "") + "/auth/oauth-callback#" + fragment;
        log.debug("OAuth success redirect to SPA callback");
        response.sendRedirect(target);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
