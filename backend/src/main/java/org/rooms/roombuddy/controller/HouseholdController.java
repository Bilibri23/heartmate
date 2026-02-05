package org.rooms.roombuddy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombuddy.dto.request.*;
import org.rooms.roombuddy.dto.response.*;
import org.rooms.roombuddy.service.HouseholdService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/households")
@RequiredArgsConstructor
@Slf4j
public class HouseholdController {
    
    private final HouseholdService householdService;
    
    // ==================== HOUSEHOLD ENDPOINTS ====================
    
    /**
     * Internal endpoint for creating household from an active lease.
     * Called automatically when a shared lease becomes active.
     * Not intended for direct user access.
     */
    @PostMapping("/from-lease/{leaseId}")
    public ResponseEntity<HouseholdResponse> createFromLease(@PathVariable UUID leaseId) {
        log.info("Creating household from lease: {}", leaseId);
        return ResponseEntity.ok(householdService.createHouseholdFromLease(leaseId));
    }
    
    @GetMapping
    public ResponseEntity<List<HouseholdResponse>> getMyHouseholds(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(householdService.getMyHouseholds(userId));
    }
    
    @GetMapping("/{householdId}")
    public ResponseEntity<HouseholdResponse> getHousehold(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(householdService.getHousehold(householdId, userId));
    }
    
    @PostMapping("/{householdId}/invite")
    public ResponseEntity<Void> inviteMember(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId,
            @RequestBody Map<String, Object> request) {
        UUID inviteeId = UUID.fromString((String) request.get("userId"));
        Integer rentShare = request.containsKey("rentShare") ? (Integer) request.get("rentShare") : null;
        householdService.inviteMember(householdId, userId, inviteeId, rentShare);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{householdId}/invitation/respond")
    public ResponseEntity<Void> respondToInvitation(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId,
            @RequestBody Map<String, Boolean> request) {
        householdService.respondToInvitation(householdId, userId, request.get("accept"));
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{householdId}/leave")
    public ResponseEntity<Void> leaveHousehold(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId) {
        householdService.leavHousehold(householdId, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Delete a household. Only the household admin can delete, 
     * and only if the household has no active lease.
     */
    @DeleteMapping("/{householdId}")
    public ResponseEntity<Void> deleteHousehold(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId) {
        log.info("Deleting household: {} by user: {}", householdId, userId);
        householdService.deleteHousehold(householdId, userId);
        return ResponseEntity.ok().build();
    }
    
    // ==================== EXPENSE ENDPOINTS ====================
    
    @PostMapping("/{householdId}/expenses")
    public ResponseEntity<ExpenseResponse> createExpense(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateExpenseRequest request) {
        log.info("Creating expense in household: {} by user: {}", householdId, userId);
        return ResponseEntity.ok(householdService.createExpense(householdId, userId, request));
    }
    
    @GetMapping("/{householdId}/expenses")
    public ResponseEntity<Page<ExpenseResponse>> getExpenses(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(householdService.getHouseholdExpenses(householdId, userId, pageable));
    }
    
    @PostMapping("/expenses/splits/{splitId}/mark-paid")
    public ResponseEntity<Void> markSplitAsPaid(
            @PathVariable UUID splitId,
            @AuthenticationPrincipal UUID userId) {
        householdService.markSplitAsPaid(splitId, userId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{householdId}/settlements")
    public ResponseEntity<ExpenseResponse> recordSettlement(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SettlementRequest request) {
        return ResponseEntity.ok(householdService.recordSettlement(householdId, userId, request));
    }
    
    @GetMapping("/{householdId}/balance")
    public ResponseEntity<BalanceSummaryResponse> getBalanceSummary(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(householdService.getBalanceSummary(householdId, userId));
    }
    
    // ==================== HOUSE RULES ENDPOINTS ====================
    
    @GetMapping("/{householdId}/rules")
    public ResponseEntity<HouseRulesResponse> getHouseRules(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(householdService.getOrCreateHouseRules(householdId, userId));
    }
    
    @PutMapping("/{householdId}/rules")
    public ResponseEntity<HouseRulesResponse> updateHouseRules(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody HouseRulesRequest request) {
        return ResponseEntity.ok(householdService.updateHouseRules(householdId, userId, request));
    }
    
    @PostMapping("/{householdId}/rules/agree")
    public ResponseEntity<Void> agreeToRules(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId) {
        householdService.agreeToRules(householdId, userId);
        return ResponseEntity.ok().build();
    }
    
    // ==================== BILL REMINDER ENDPOINTS ====================
    
    @PostMapping("/{householdId}/bill-reminders")
    public ResponseEntity<BillReminderResponse> createBillReminder(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateBillReminderRequest request) {
        return ResponseEntity.ok(householdService.createBillReminder(householdId, userId, request));
    }
    
    @GetMapping("/{householdId}/bill-reminders")
    public ResponseEntity<List<BillReminderResponse>> getBillReminders(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(householdService.getHouseholdBillReminders(householdId, userId));
    }
    
    // ==================== TASK ENDPOINTS ====================
    
    @PostMapping("/{householdId}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateTaskRequest request) {
        log.info("Creating task in household: {} by user: {}", householdId, userId);
        return ResponseEntity.ok(householdService.createTask(householdId, userId, request));
    }
    
    @GetMapping("/{householdId}/tasks")
    public ResponseEntity<List<TaskResponse>> getTasks(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(householdService.getHouseholdTasks(householdId, userId));
    }
    
    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskResponse>> getMyTasks(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(householdService.getMyTasks(userId));
    }
    
    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<TaskResponse> completeTask(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(householdService.completeTask(taskId, userId));
    }
    
    // ==================== ISSUE ENDPOINTS ====================
    
    @PostMapping("/{householdId}/issues")
    public ResponseEntity<IssueResponse> createIssue(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateIssueRequest request) {
        log.info("Creating issue in household: {} by user: {}", householdId, userId);
        return ResponseEntity.ok(householdService.createIssue(householdId, userId, request));
    }
    
    @GetMapping("/{householdId}/issues")
    public ResponseEntity<List<IssueResponse>> getIssues(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(householdService.getHouseholdIssues(householdId, userId));
    }
    
    @PostMapping("/issues/{issueId}/comments")
    public ResponseEntity<IssueResponse> addComment(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal UUID userId,
            @RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        boolean isResolutionProposal = request.containsKey("isResolutionProposal") && 
                (Boolean) request.get("isResolutionProposal");
        return ResponseEntity.ok(householdService.addComment(issueId, userId, content, isResolutionProposal));
    }
    
    @PostMapping("/issues/{issueId}/resolve")
    public ResponseEntity<IssueResponse> resolveIssue(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal UUID userId,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(householdService.resolveIssue(issueId, userId, request.get("resolution")));
    }
    
    @PostMapping("/issues/{issueId}/escalate")
    public ResponseEntity<Void> escalateIssue(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal UUID userId,
            @RequestBody Map<String, String> request) {
        householdService.escalateIssue(issueId, userId, request.get("escalateTo"));
        return ResponseEntity.ok().build();
    }
}
