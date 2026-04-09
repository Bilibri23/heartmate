package org.rooms.roombay.repository;

import org.rooms.roombay.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByOauthProviderAndOauthSubject(String oauthProvider, String oauthSubject);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    
    // Count methods
    long countByRole(User.UserRole role);
    long countByAccountStatus(User.AccountStatus accountStatus);
    
    // Find by role and status with pagination
    Page<User> findByRole(User.UserRole role, Pageable pageable);
    Page<User> findByAccountStatus(User.AccountStatus accountStatus, Pageable pageable);
    
    // Search users by name, email, or phone
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);
    
    // Analytics methods
    long countByEmailVerifiedAndPhoneVerified(Boolean emailVerified, Boolean phoneVerified);
    
    long countByProfileCompleted(Boolean profileCompleted);
    
    long countByCreatedAtAfter(LocalDateTime date);
}

