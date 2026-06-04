package org.rooms.roombay.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final org.rooms.roombay.service.PlatformSettingsService platformSettingsService;

    @Value("${app.verification.enforce-for-api:false}")
    private boolean verificationEnforceForApi;
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        boolean isApiPath = path != null && path.startsWith("/api/");
        boolean isPublicApi = isApiPath && (
                path.startsWith("/api/auth/") ||
                path.startsWith("/api/phone-verification/") ||
                path.startsWith("/api/search") ||
                path.startsWith("/api/feed") ||
                path.startsWith("/api/listings") ||
                path.startsWith("/api/ai/")
        );
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // Treat missing "type" claim as access (older tokens); only skip when explicitly non-access (e.g. refresh).
                String tokenType = jwtTokenProvider.getTokenType(jwt);
                if (tokenType != null && !"access".equalsIgnoreCase(tokenType)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                UUID userId = jwtTokenProvider.getUserIdFromToken(jwt);
                String role = jwtTokenProvider.getRoleFromToken(jwt);

                if (verificationEnforceForApi && isVerificationEnforcedPath(request.getRequestURI())) {
                    User user = userRepository.findById(userId).orElse(null);
                    if (user == null || !Boolean.TRUE.equals(user.getEmailVerified()) || !Boolean.TRUE.equals(user.getPhoneVerified())) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"success\":false,\"message\":\"Email and phone verification required.\"}");
                        return;
                    }
                }
                
                // Create authority from role (ensure it starts with ROLE_)
                String authority = role != null && role.startsWith("ROLE_") 
                        ? role 
                        : "ROLE_" + (role != null ? role : "USER");
                
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(authority))
                );
                
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Maintenance mode: block non-admin authenticated users
                if (Boolean.TRUE.equals(platformSettingsService.getRawSettings().getMaintenanceMode())) {
                    String tokenRole = jwtTokenProvider.getRoleFromToken(jwt);
                    boolean isAdmin = tokenRole != null && (tokenRole.equals("ADMIN") || tokenRole.equals("ROLE_ADMIN"));
                    if (!isAdmin) {
                        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"success\":false,\"message\":\"Platform is under maintenance. Please try again later.\"}");
                        return;
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }
        
        // If this is a protected API path and no authentication was set, return 401
        // instead of letting it fall through to OAuth2 redirect (which causes CORS issues)
        if (isApiPath && !isPublicApi && SecurityContextHolder.getContext().getAuthentication() == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.getWriter().write("{\"success\":false,\"message\":\"Authentication required. Please log in.\"}");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isVerificationEnforcedPath(String path) {
        if (path == null) {
            return false;
        }
        if (!path.startsWith("/api/")) {
            return false;
        }
        return !path.startsWith("/api/auth/")
                && !path.startsWith("/api/phone-verification/");
    }
}

