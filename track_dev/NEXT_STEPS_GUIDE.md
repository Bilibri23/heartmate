# Next Steps Guide - Quick Start

This guide provides a step-by-step approach to completing Sprint 1, with code templates and examples.

---

## 🎯 Priority 1: Authentication (START HERE)

### Step 1: Create Authentication DTOs (30 minutes)

#### 1.1 RegisterRequest.java
```java
package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+237[0-9]{9}$", message = "Phone must be in format +237XXXXXXXXX")
    private String phone;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    private String gender; // MALE, FEMALE
    
    private LocalDate dateOfBirth;
}
```

#### 1.2 LoginRequest.java
```java
package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "Email or phone is required")
    private String emailOrPhone; // Can be email or phone
    
    @NotBlank(message = "Password is required")
    private String password;
}
```

#### 1.3 AuthResponse.java
```java
package org.rooms.roombuddy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UUID userId;
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // seconds
}
```

#### 1.4 PasswordResetRequest.java
```java
package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
```

#### 1.5 ResetPasswordRequest.java
```java
package org.rooms.roombuddy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    
    @NotBlank(message = "Token is required")
    private String token;
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String newPassword;
}
```

---

### Step 2: Create JWT Token Provider (2-3 hours)

#### 2.1 Add JWT Dependencies to pom.xml
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

#### 2.2 JwtTokenProvider.java
```java
package org.rooms.roombuddy.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {
    
    @Value("${app.jwt.secret:your-256-bit-secret-key-change-in-production-minimum-32-characters}")
    private String jwtSecret;
    
    @Value("${app.jwt.access-token-expiration:3600000}") // 1 hour in milliseconds
    private long accessTokenExpiration;
    
    @Value("${app.jwt.refresh-token-expiration:604800000}") // 7 days in milliseconds
    private long refreshTokenExpiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    public String generateAccessToken(UUID userId, String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);
        
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);
        
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
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
}
```

---

### Step 3: Create AuthService (2-3 hours)

#### 3.1 AuthService.java
```java
package org.rooms.roombuddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.LoginRequest;
import org.rooms.roombuddy.dto.request.RegisterRequest;
import org.rooms.roombuddy.dto.response.AuthResponse;
import org.rooms.roombuddy.entity.User;
import org.rooms.roombuddy.exception.BadRequestException;
import org.rooms.roombuddy.exception.ResourceNotFoundException;
import org.rooms.roombuddy.repository.UserRepository;
import org.rooms.roombuddy.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        
        // Check if phone already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number already registered");
        }
        
        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .gender(User.Gender.valueOf(request.getGender()))
                .dateOfBirth(request.getDateOfBirth())
                .role(User.UserRole.STUDENT)
                .accountStatus(User.AccountStatus.PENDING)
                .emailVerified(false)
                .phoneVerified(false)
                .profileCompleted(false)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());
        
        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getId());
        
        return AuthResponse.builder()
                .userId(savedUser.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L) // 1 hour in seconds
                .build();
    }
    
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmailOrPhone());
        
        // Determine if email or phone
        User user;
        if (EMAIL_PATTERN.matcher(request.getEmailOrPhone()).matches()) {
            user = userRepository.findByEmail(request.getEmailOrPhone())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));
        } else {
            user = userRepository.findByPhone(request.getEmailOrPhone())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }
        
        // Check if account is active
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE && 
            user.getAccountStatus() != User.AccountStatus.PENDING) {
            throw new BadRequestException("Account is " + user.getAccountStatus());
        }
        
        // Update last active
        user.setLastActive(LocalDateTime.now());
        userRepository.save(user);
        
        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        
        log.info("User logged in successfully: {}", user.getId());
        
        return AuthResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600L)
                .build();
    }
    
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }
        
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Generate new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        
        return AuthResponse.builder()
                .userId(user.getId())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(3600L)
                .build();
    }
}
```

---

### Step 4: Create AuthController (1 hour)

#### 4.1 AuthController.java
```java
package org.rooms.roombuddy.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.LoginRequest;
import org.rooms.roombuddy.dto.request.PasswordResetRequest;
import org.rooms.roombuddy.dto.request.RegisterRequest;
import org.rooms.roombuddy.dto.request.ResetPasswordRequest;
import org.rooms.roombuddy.dto.response.AuthResponse;
import org.rooms.roombuddy.dto.response.ApiResponse;
import org.rooms.roombuddy.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "APIs for user authentication")
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Register a new student user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Login with email or phone and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for: {}", request.getEmailOrPhone());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        String refreshToken = authHeader.replace("Bearer ", "");
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Request password reset")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        // TODO: Implement password reset
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Password reset link sent to email")
                .build());
    }
    
    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Reset password using token")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        // TODO: Implement password reset
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Password reset successfully")
                .build());
    }
}
```

---

### Step 5: Update SecurityConfig (1 hour)

#### 5.1 Update SecurityConfig.java
```java
package org.rooms.roombuddy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/profiles/**").authenticated() // Require auth for profiles
                        .anyRequest().authenticated()
                );
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
```

---

### Step 6: Update application.properties

```properties
# JWT Configuration
app.jwt.secret=your-256-bit-secret-key-change-in-production-minimum-32-characters-long
app.jwt.access-token-expiration=3600000
app.jwt.refresh-token-expiration=604800000

# Email Configuration (for password reset)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

## 🎯 Priority 2: Student Verification

### Step 1: Create Database Migration

#### V3__create_student_verification_table.sql
```sql
-- =====================================================
-- STUDENT VERIFICATION TABLE
-- =====================================================
CREATE TABLE student_verification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    university VARCHAR(100) NOT NULL,
    student_id VARCHAR(50) NOT NULL,
    faculty VARCHAR(100),
    department VARCHAR(100),
    year_of_study INTEGER,
    student_id_photo_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    rejection_reason TEXT,
    verified_by UUID,
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_student_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_student_verification_verifier FOREIGN KEY (verified_by) REFERENCES users(id)
);

-- Create indexes
CREATE INDEX idx_student_verification_user_id ON student_verification(user_id);
CREATE INDEX idx_student_verification_status ON student_verification(status);

-- Comments
COMMENT ON TABLE student_verification IS 'Student verification requests';
COMMENT ON COLUMN student_verification.status IS 'PENDING: awaiting review, VERIFIED: approved, REJECTED: denied';
```

### Step 2: Create Entity

#### StudentVerification.java
```java
package org.rooms.roombuddy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_verification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(nullable = false)
    private String university;
    
    @Column(name = "student_id", nullable = false)
    private String studentId;
    
    private String faculty;
    
    private String department;
    
    @Column(name = "year_of_study")
    private Integer yearOfStudy;
    
    @Column(name = "student_id_photo_url")
    private String studentIdPhotoUrl;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;
    
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum Status {
        PENDING, VERIFIED, REJECTED
    }
}
```

---

## 📝 Testing Checklist

After implementing each feature, test with Postman:

### Authentication
- [ ] POST /api/auth/register - Register new user
- [ ] POST /api/auth/login - Login with email
- [ ] POST /api/auth/login - Login with phone
- [ ] POST /api/auth/refresh - Refresh token
- [ ] Verify JWT token in profile endpoints

### Student Verification
- [ ] POST /api/verifications - Submit verification
- [ ] GET /api/verifications/{userId} - Get verification status
- [ ] Verify file upload works

---

## 🚀 Quick Start Commands

```bash
# Add JWT dependencies
# Update pom.xml with jjwt dependencies

# Create DTOs
# Create all DTO files in dto/request and dto/response

# Create JWT provider
# Create security/JwtTokenProvider.java

# Create AuthService
# Create service/AuthService.java

# Create AuthController
# Create controller/AuthController.java

# Update SecurityConfig
# Update config/SecurityConfig.java

# Update application.properties
# Add JWT and email configuration

# Test
./mvnw spring-boot:run
```

---

## ⚠️ Common Issues & Solutions

### Issue: JWT token validation fails
**Solution:** Ensure secret key is at least 32 characters for HS256

### Issue: Password encoding doesn't match
**Solution:** Use the same PasswordEncoder bean everywhere

### Issue: CORS errors
**Solution:** Add CORS configuration to SecurityConfig

### Issue: Token expired
**Solution:** Check token expiration settings in application.properties

---

**Last Updated:** $(date)
**Next Steps:** Implement authentication, then student verification

