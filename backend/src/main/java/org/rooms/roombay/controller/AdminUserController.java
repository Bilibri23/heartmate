package org.rooms.roombay.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.UserSuspensionRequest;
import org.rooms.roombay.dto.request.UserUpdateRequest;
import org.rooms.roombay.dto.response.UserManagementResponse;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.security.SecurityUtils;
import org.rooms.roombay.service.UserManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin User Management", description = "Admin APIs for managing users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    
    private final UserManagementService userManagementService;
    
    /**
     * Get all users with pagination and optional filters
     */
    @GetMapping
    @Operation(summary = "Get all users", description = "Get all users with pagination and optional filters (Admin only)")
    public ResponseEntity<Page<UserManagementResponse>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        log.info("Admin {} fetching users - role: {}, status: {}, search: {}, page: {}, size: {}",
                SecurityUtils.getCurrentUserId(), role, status, search, page, size);
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<UserManagementResponse> users;
        
        if (search != null && !search.trim().isEmpty()) {
            users = userManagementService.searchUsers(search, pageable);
        } else if (role != null) {
            User.UserRole userRole = User.UserRole.valueOf(role.toUpperCase());
            users = userManagementService.getUsersByRole(userRole, pageable);
        } else if (status != null) {
            User.AccountStatus accountStatus = User.AccountStatus.valueOf(status.toUpperCase());
            users = userManagementService.getUsersByStatus(accountStatus, pageable);
        } else {
            users = userManagementService.getAllUsers(pageable);
        }
        
        return ResponseEntity.ok(users);
    }
    
    /**
     * Get user by ID
     */
    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Get detailed user information (Admin only)")
    public ResponseEntity<UserManagementResponse> getUserById(@PathVariable UUID userId) {
        log.info("Admin {} fetching user: {}", SecurityUtils.getCurrentUserId(), userId);
        UserManagementResponse user = userManagementService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Update user
     */
    @PutMapping("/{userId}")
    @Operation(summary = "Update user", description = "Update user details (Admin only)")
    public ResponseEntity<UserManagementResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("Admin {} updating user: {}", SecurityUtils.getCurrentUserId(), userId);
        UserManagementResponse updatedUser = userManagementService.updateUser(userId, request);
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * Suspend user
     */
    @PostMapping("/{userId}/suspend")
    @Operation(summary = "Suspend user", description = "Suspend user account (Admin only)")
    public ResponseEntity<UserManagementResponse> suspendUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserSuspensionRequest request) {
        log.info("Admin {} suspending user: {}", SecurityUtils.getCurrentUserId(), userId);
        UUID adminId = SecurityUtils.getCurrentUserId();
        UserManagementResponse suspendedUser = userManagementService.suspendUser(userId, adminId, request);
        return ResponseEntity.ok(suspendedUser);
    }
    
    /**
     * Activate user
     */
    @PostMapping("/{userId}/activate")
    @Operation(summary = "Activate user", description = "Activate/unsuspend user account (Admin only)")
    public ResponseEntity<UserManagementResponse> activateUser(@PathVariable UUID userId) {
        log.info("Admin {} activating user: {}", SecurityUtils.getCurrentUserId(), userId);
        UUID adminId = SecurityUtils.getCurrentUserId();
        UserManagementResponse activatedUser = userManagementService.activateUser(userId, adminId);
        return ResponseEntity.ok(activatedUser);
    }
    
    /**
     * Delete user (soft delete)
     */
    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Delete user account (soft delete - deactivate) (Admin only)")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
        log.info("Admin {} deleting user: {}", SecurityUtils.getCurrentUserId(), userId);
        UUID adminId = SecurityUtils.getCurrentUserId();
        userManagementService.deleteUser(userId, adminId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get user statistics by role
     */
    @GetMapping("/stats/by-role")
    @Operation(summary = "Get user stats by role", description = "Get user count statistics by role (Admin only)")
    public ResponseEntity<Map<String, Long>> getUserStatsByRole() {
        log.info("Admin {} fetching user stats by role", SecurityUtils.getCurrentUserId());
        Map<String, Long> stats = userManagementService.getUserStatsByRole();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get user statistics by status
     */
    @GetMapping("/stats/by-status")
    @Operation(summary = "Get user stats by status", description = "Get user count statistics by status (Admin only)")
    public ResponseEntity<Map<String, Long>> getUserStatsByStatus() {
        log.info("Admin {} fetching user stats by status", SecurityUtils.getCurrentUserId());
        Map<String, Long> stats = userManagementService.getUserStatsByStatus();
        return ResponseEntity.ok(stats);
    }
}
