package org.rooms.roombuddy.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.rooms.roombuddy.config.BackendEnvFileSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${spring.jwt.secret}")
    private String jwtSecret;

    @Value("${spring.jwt.access-token-expiration}") // 1 hour in milliseconds
    private long accessTokenExpiration;

    @Value("${spring.jwt.refresh-token-expiration}") // 7 days in milliseconds
    private long refreshTokenExpiration;

    @PostConstruct
    void validateJwtSecretAtStartup() {
        // If OS env has JWT_SECRET="" it "wins" over backend/.env in PropertySources; read file last-resort.
        if (!isAcceptableJwtSecret(jwtSecret)) {
            BackendEnvFileSupport.readProperty("JWT_SECRET").ifPresent(fromFile -> {
                if (isAcceptableJwtSecret(fromFile)) {
                    this.jwtSecret = fromFile.trim();
                    log.warn("JWT_SECRET was missing or too short in Spring env; using JWT_SECRET from backend/.env");
                }
            });
        }
        getSigningKey();
    }

    private static boolean isAcceptableJwtSecret(String secret) {
        if (!StringUtils.hasText(secret)) {
            return false;
        }
        return secret.trim().getBytes(StandardCharsets.UTF_8).length >= 32;
    }

    private SecretKey getSigningKey() {
        String secret = StringUtils.hasText(jwtSecret) ? jwtSecret.trim() : "";
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "spring.jwt.secret / JWT_SECRET is missing or shorter than 32 bytes (256 bits). "
                            + "Set JWT_SECRET in backend/.env (repo root) or OS environment; "
                            + "IDE launch.json env is not used when you run via mvn spring-boot:run.");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "access")
                .claim("email", email)
                .claim("role", role)
                .claim("jti", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(UUID userId, UUID sessionId, UUID tokenId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .claim("sid", sessionId.toString())
                .claim("jti", tokenId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return UUID.fromString(claims.getSubject());
    }
    
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("role", String.class);
    }
    
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getTokenType(String token) {
        return getClaimsFromToken(token).get("type", String.class);
    }

    public UUID getTokenId(String token) {
        String value = getClaimsFromToken(token).get("jti", String.class);
        return value != null ? UUID.fromString(value) : null;
    }

    public UUID getSessionId(String token) {
        String value = getClaimsFromToken(token).get("sid", String.class);
        return value != null ? UUID.fromString(value) : null;
    }
}
