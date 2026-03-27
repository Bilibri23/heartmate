package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.UserSuspensionRequest;
import org.rooms.roombay.dto.request.UserUpdateRequest;
import org.rooms.roombay.dto.response.UserManagementResponse;
import org.rooms.roombay.entity.StudentVerification;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.StudentVerificationRepository;
import org.rooms.roombay.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {
    
    private final UserRepository userRepository;
    private final PropertyListingRepository listingRepository;
    private final StudentVerificationRepository verificationRepository;
    
    /**
     * Get all users with pagination
     */
    public Page<UserManagementResponse> getAllUsers(Pageable pageable) {
        log.info("Fetching all users with pagination: {}", pageable);
        return userRepository.findAll(pageable)
                .map(this::mapToUserManagementResponse);
    }
    
    /**
     * Get users by role with pagination
     */
    public Page<UserManagementResponse> getUsersByRole(User.UserRole role, Pageable pageable) {
        log.info("Fetching users by role: {} with pagination: {}", role, pageable);
        return userRepository.findByRole(role, pageable)
                .map(this::mapToUserManagementResponse);
    }
    
    /**
     * Get users by account status with pagination
     */
    public Page<UserManagementResponse> getUsersByStatus(User.AccountStatus status, Pageable pageable) {
        log.info("Fetching users by status: {} with pagination: {}", status, pageable);
        return userRepository.findByAccountStatus(status, pageable)
                .map(this::mapToUserManagementResponse);
    }
    
    /**
     * Search users by name, email, or phone
     */
    public Page<UserManagementResponse> searchUsers(String query, Pageable pageable) {
        log.info("Searching users with query: {}", query);
        return userRepository.searchUsers(query, pageable)
                .map(this::mapToUserManagementResponse);
    }
    
    /**
     * Get user by ID
     */
    public UserManagementResponse getUserById(UUID userId) {
        log.info("Fetching user by ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return mapToUserManagementResponse(user);
    }
    
    /**
     * Update user details
     */
    @Transactional
    public UserManagementResponse updateUser(UUID userId, UserUpdateRequest request) {
        log.info("Updating user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // Update fields if provided
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getAccountStatus() != null) {
            user.setAccountStatus(request.getAccountStatus());
        }
        
        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", userId);
        
        return mapToUserManagementResponse(updatedUser);
    }
    
    /**
     * Suspend user account
     */
    @Transactional
    public UserManagementResponse suspendUser(UUID userId, UUID adminId, UserSuspensionRequest request) {
        log.info("Suspending user: {} by admin: {}", userId, adminId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (user.getAccountStatus() == User.AccountStatus.SUSPENDED) {
            throw new IllegalStateException("User is already suspended");
        }
        
        user.setAccountStatus(User.AccountStatus.SUSPENDED);
        user.setSuspended(true);
        user.setSuspendedAt(LocalDateTime.now());
        user.setSuspendedBy(adminId);
        user.setSuspensionReason(request.getReason());
        
        User suspendedUser = userRepository.save(user);
        log.info("User suspended successfully: {}", userId);
        
        return mapToUserManagementResponse(suspendedUser);
    }
    
    /**
     * Activate/Unsuspend user account
     */
    @Transactional
    public UserManagementResponse activateUser(UUID userId, UUID adminId) {
        log.info("Activating user: {} by admin: {}", userId, adminId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        user.setSuspended(false);
        user.setSuspendedAt(null);
        user.setSuspendedBy(null);
        user.setSuspensionReason(null);
        
        User activatedUser = userRepository.save(user);
        log.info("User activated successfully: {}", userId);
        
        return mapToUserManagementResponse(activatedUser);
    }
    
    /**
     * Delete user (soft delete by deactivating)
     */
    @Transactional
    public void deleteUser(UUID userId, UUID adminId) {
        log.info("Deleting user: {} by admin: {}", userId, adminId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setAccountStatus(User.AccountStatus.DEACTIVATED);
        userRepository.save(user);
        
        log.info("User deleted (deactivated) successfully: {}", userId);
    }
    
    /**
     * Get user statistics by role
     */
    public Map<String, Long> getUserStatsByRole() {
        log.info("Fetching user statistics by role");
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("STUDENT", userRepository.countByRole(User.UserRole.STUDENT));
        stats.put("LANDLORD", userRepository.countByRole(User.UserRole.LANDLORD));
        stats.put("ADMIN", userRepository.countByRole(User.UserRole.ADMIN));
        stats.put("TOTAL", userRepository.count());
        
        return stats;
    }
    
    /**
     * Get user statistics by status
     */
    public Map<String, Long> getUserStatsByStatus() {
        log.info("Fetching user statistics by status");
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("PENDING", userRepository.countByAccountStatus(User.AccountStatus.PENDING));
        stats.put("ACTIVE", userRepository.countByAccountStatus(User.AccountStatus.ACTIVE));
        stats.put("SUSPENDED", userRepository.countByAccountStatus(User.AccountStatus.SUSPENDED));
        stats.put("DEACTIVATED", userRepository.countByAccountStatus(User.AccountStatus.DEACTIVATED));
        
        return stats;
    }
    
    /**
     * Map User entity to UserManagementResponse DTO
     */
    private UserManagementResponse mapToUserManagementResponse(User user) {
        // Count user's listings if landlord
        Long listingsCount = 0L;
        if (user.getRole() == User.UserRole.LANDLORD) {
            listingsCount = listingRepository.countByLandlordId(user.getId());
        }
        
        // Count user's verifications if student
        Long verificationsCount = 0L;
        if (user.getRole() == User.UserRole.STUDENT) {
            verificationsCount = verificationRepository.countByUserId(user.getId());
        }
        
        return UserManagementResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .profileCompleted(user.getProfileCompleted())
                .lastActive(user.getLastActive())
                .suspendedAt(user.getSuspendedAt())
                .suspendedBy(user.getSuspendedBy())
                .suspensionReason(user.getSuspensionReason())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .listingsCount(listingsCount)
                .verificationsCount(verificationsCount)
                .isSuspended(user.getAccountStatus() == User.AccountStatus.SUSPENDED)
                .build();
    }
}
