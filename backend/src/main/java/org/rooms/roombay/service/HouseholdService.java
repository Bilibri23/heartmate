package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.*;
import org.rooms.roombay.dto.response.*;
import org.rooms.roombay.entity.*;
import org.rooms.roombay.exception.BadRequestException;
import org.rooms.roombay.exception.ResourceNotFoundException;
import org.rooms.roombay.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HouseholdService {
    
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository memberRepository;
    private final HouseholdExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final ExpenseSettlementRepository settlementRepository;
    private final HouseRulesRepository rulesRepository;
    private final RuleAgreementRepository agreementRepository;
    private final BillReminderRepository billReminderRepository;
    private final HouseholdTaskRepository taskRepository;
    private final HouseholdIssueRepository issueRepository;
    private final IssueCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final LeaseRepository leaseRepository;
    private final PropertyListingRepository listingRepository;
    private final NotificationService notificationService;
    private final ProfileRepository profileRepository;
    
    // ==================== HOUSEHOLD MANAGEMENT ====================
    
    public HouseholdResponse createHousehold(UUID userId, CreateHouseholdRequest request) {
        log.info("Creating household: {} by user: {}", request.getName(), userId);
        
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Household household = Household.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(Household.HouseholdStatus.ACTIVE)
                .build();
        
        // Link to lease if provided
        if (request.getLeaseId() != null) {
            Lease lease = leaseRepository.findById(request.getLeaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
            household.setLease(lease);
            household.setListing(lease.getListing());
        } else if (request.getListingId() != null) {
            PropertyListing listing = listingRepository.findById(request.getListingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
            household.setListing(listing);
        }
        
        household = householdRepository.save(household);
        
        // Add creator as admin member
        HouseholdMember creatorMember = HouseholdMember.builder()
                .household(household)
                .user(creator)
                .role(HouseholdMember.MemberRole.ADMIN)
                .rentSharePercentage(100)
                .inviteStatus(HouseholdMember.InviteStatus.ACCEPTED)
                .build();
        memberRepository.save(creatorMember);
        
        // Invite other members
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            int sharePerMember = 100 / (request.getMemberIds().size() + 1);
            creatorMember.setRentSharePercentage(sharePerMember);
            memberRepository.save(creatorMember);
            
            for (UUID memberId : request.getMemberIds()) {
                if (!memberId.equals(userId)) {
                    inviteMember(household.getId(), userId, memberId, sharePerMember);
                }
            }
        }
        
        return buildHouseholdResponse(household, userId);
    }
    
    public HouseholdResponse createHouseholdFromLease(UUID leaseId) {
        log.info("Auto-creating household from lease: {}", leaseId);
        
        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lease not found"));
        
        // Check if household already exists for this lease
        Optional<Household> existing = householdRepository.findByLeaseId(leaseId);
        if (existing.isPresent()) {
            return buildHouseholdResponse(existing.get(), lease.getStudent().getId());
        }
        
        // Create household
        String householdName = lease.getListing().getTitle() + " Household";
        Household household = Household.builder()
                .name(householdName)
                .lease(lease)
                .listing(lease.getListing())
                .status(Household.HouseholdStatus.ACTIVE)
                .build();
        household = householdRepository.save(household);
        
        // Determine rent share (50/50 if co-tenant, 100% if solo)
        int rentShare = lease.hasCoTenant() ? 50 : 100;
        
        // Add student as member
        HouseholdMember studentMember = HouseholdMember.builder()
                .household(household)
                .user(lease.getStudent())
                .role(HouseholdMember.MemberRole.ADMIN)
                .rentSharePercentage(rentShare)
                .inviteStatus(HouseholdMember.InviteStatus.ACCEPTED)
                .build();
        memberRepository.save(studentMember);
        
        // Add co-tenant as member if present
        if (lease.hasCoTenant()) {
            HouseholdMember coTenantMember = HouseholdMember.builder()
                    .household(household)
                    .user(lease.getCoTenant())
                    .role(HouseholdMember.MemberRole.ADMIN) // Co-tenants are also admins
                    .rentSharePercentage(rentShare)
                    .inviteStatus(HouseholdMember.InviteStatus.ACCEPTED)
                    .build();
            memberRepository.save(coTenantMember);
            
            // Notify co-tenant about household creation
            notificationService.createNotification(
                    lease.getCoTenant().getId(),
                    Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Welcome to Your Shared Household!",
                    "Your shared household '" + householdName + "' with " + lease.getStudent().getFirstName() + " has been created. You can now manage expenses, tasks, and house rules together.",
                    household.getId(), "HOUSEHOLD", "/household/" + household.getId()
            );
            log.info("Added co-tenant {} to household {}", lease.getCoTenant().getId(), household.getId());
        }
        
        // Notify student about household creation
        String studentMessage = lease.hasCoTenant() 
                ? "Your shared household '" + householdName + "' with " + lease.getCoTenant().getFirstName() + " has been created. You can now manage expenses, tasks, and house rules together."
                : "Your household '" + householdName + "' has been created. You can now manage expenses, tasks, and house rules.";
        
        notificationService.createNotification(
                lease.getStudent().getId(),
                Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                lease.hasCoTenant() ? "Welcome to Your Shared Household!" : "Welcome to Your Household!",
                studentMessage,
                household.getId(), "HOUSEHOLD", "/household/" + household.getId()
        );
        
        return buildHouseholdResponse(household, lease.getStudent().getId());
    }
    
    public List<HouseholdResponse> getMyHouseholds(UUID userId) {
        List<Household> households = householdRepository.findActiveByMemberUserId(userId);
        return households.stream()
                .map(h -> buildHouseholdResponse(h, userId))
                .collect(Collectors.toList());
    }
    
    public HouseholdResponse getHousehold(UUID householdId, UUID userId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found"));
        
        // Verify membership
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        return buildHouseholdResponse(household, userId);
    }
    
    public void inviteMember(UUID householdId, UUID inviterId, UUID inviteeId, Integer rentShare) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found"));
        
        // Verify inviter is admin
        HouseholdMember inviter = memberRepository.findActiveByHouseholdIdAndUserId(householdId, inviterId)
                .orElseThrow(() -> new BadRequestException("You are not a member of this household"));
        
        if (inviter.getRole() != HouseholdMember.MemberRole.ADMIN) {
            throw new BadRequestException("Only admins can invite members");
        }
        
        User invitee = userRepository.findById(inviteeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Check if already a member
        if (memberRepository.findByHouseholdIdAndUserId(householdId, inviteeId).isPresent()) {
            throw new BadRequestException("User is already a member or has been invited");
        }
        
        HouseholdMember newMember = HouseholdMember.builder()
                .household(household)
                .user(invitee)
                .role(HouseholdMember.MemberRole.MEMBER)
                .rentSharePercentage(rentShare != null ? rentShare : 50)
                .inviteStatus(HouseholdMember.InviteStatus.PENDING)
                .build();
        memberRepository.save(newMember);
        
        // Notify invitee
        notificationService.createNotification(
                inviteeId,
                Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                "Household Invitation",
                "You've been invited to join the household: " + household.getName(),
                household.getId(), "HOUSEHOLD", "/household/" + household.getId()
        );
    }
    
    public void respondToInvitation(UUID householdId, UUID userId, boolean accept) {
        HouseholdMember member = memberRepository.findByHouseholdIdAndUserId(householdId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        
        if (member.getInviteStatus() != HouseholdMember.InviteStatus.PENDING) {
            throw new BadRequestException("Invitation already responded to");
        }
        
        if (accept) {
            member.setInviteStatus(HouseholdMember.InviteStatus.ACCEPTED);
            member.setJoinedAt(LocalDateTime.now());
        } else {
            member.setInviteStatus(HouseholdMember.InviteStatus.DECLINED);
        }
        memberRepository.save(member);
    }
    
    public void leavHousehold(UUID householdId, UUID userId) {
        HouseholdMember member = memberRepository.findActiveByHouseholdIdAndUserId(householdId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));
        
        // Check for unpaid balances
        Long owed = splitRepository.getTotalOwedInHousehold(householdId, userId);
        if (owed != null && owed > 0) {
            throw new BadRequestException("You have unpaid balances. Please settle all expenses before leaving.");
        }
        
        member.setLeftAt(LocalDateTime.now());
        memberRepository.save(member);
        
        // If last member, archive household
        if (memberRepository.countActiveMembers(householdId) == 0) {
            Household household = householdRepository.findById(householdId).get();
            household.setStatus(Household.HouseholdStatus.ARCHIVED);
            householdRepository.save(household);
        }
    }
    
    @Transactional
    public void deleteHousehold(UUID householdId, UUID userId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found"));
        
        // Verify admin permission
        HouseholdMember member = memberRepository.findActiveByHouseholdIdAndUserId(householdId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this household"));
        
        if (member.getRole() != HouseholdMember.MemberRole.ADMIN) {
            throw new BadRequestException("Only the household admin can delete a household");
        }
        
        // Cannot delete if linked to an active lease
        if (household.getLease() != null) {
            throw new BadRequestException("Cannot delete a household linked to a lease. Please wait until the lease ends.");
        }
        
        // Delete all related data manually (foreign key constraints)
        // 1. Delete issue comments first
        List<HouseholdIssue> issues = issueRepository.findByHouseholdId(householdId);
        for (HouseholdIssue issue : issues) {
            commentRepository.deleteAll(commentRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId()));
        }
        
        // 2. Delete issues
        issueRepository.deleteAll(issues);
        
        // 3. Delete tasks
        taskRepository.deleteAll(taskRepository.findByHouseholdId(householdId));
        
        // 4. Delete bill reminders
        billReminderRepository.deleteAll(billReminderRepository.findByHouseholdId(householdId));
        
        // 5. Delete house rules and agreements
        rulesRepository.findByHouseholdId(householdId).ifPresent(rules -> {
            agreementRepository.deleteAll(rules.getAgreements());
            rulesRepository.delete(rules);
        });
        
        // 6. Delete expense splits and expenses
        List<HouseholdExpense> expenses = expenseRepository.findByHouseholdId(householdId);
        for (HouseholdExpense expense : expenses) {
            splitRepository.deleteAll(splitRepository.findByExpenseId(expense.getId()));
        }
        expenseRepository.deleteAll(expenses);
        
        // 7. Delete settlements
        settlementRepository.deleteAll(settlementRepository.findByHouseholdId(householdId));
        
        // 8. Delete members
        memberRepository.deleteAll(memberRepository.findByHouseholdId(householdId));
        
        // 9. Finally delete household
        householdRepository.delete(household);
        log.info("Household {} deleted by user {}", householdId, userId);
    }
    
    // ==================== EXPENSE MANAGEMENT ====================
    
    public ExpenseResponse createExpense(UUID householdId, UUID userId, CreateExpenseRequest request) {
        log.info("Creating expense in household: {} by user: {}", householdId, userId);
        
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found"));
        
        // Verify membership
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        User paidBy = userRepository.findById(userId).get();
        
        HouseholdExpense expense = HouseholdExpense.builder()
                .household(household)
                .paidBy(paidBy)
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .receiptUrl(request.getReceiptUrl())
                .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now())
                .splitType(request.getSplitType())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .recurringDay(request.getRecurringDay())
                .build();
        
        expense = expenseRepository.save(expense);
        
        // Create splits
        createExpenseSplits(expense, request);
        
        // Notify other members
        List<HouseholdMember> members = memberRepository.findActiveByHouseholdId(householdId);
        for (HouseholdMember member : members) {
            if (!member.getUser().getId().equals(userId)) {
                notificationService.createNotification(
                        member.getUser().getId(),
                        Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                        "New Expense Added",
                        paidBy.getFirstName() + " added an expense: " + request.getDescription() + " (" + formatCurrency(request.getAmount()) + ")",
                        expense.getId(), "EXPENSE", "/household/" + householdId
                );
            }
        }
        
        return buildExpenseResponse(expense);
    }
    
    private void createExpenseSplits(HouseholdExpense expense, CreateExpenseRequest request) {
        List<HouseholdMember> activeMembers = memberRepository.findActiveByHouseholdId(expense.getHousehold().getId());
        
        switch (request.getSplitType()) {
            case EQUAL:
                int amountPerPerson = expense.getAmount() / activeMembers.size();
                int remainder = expense.getAmount() % activeMembers.size();
                
                for (int i = 0; i < activeMembers.size(); i++) {
                    HouseholdMember member = activeMembers.get(i);
                    int splitAmount = amountPerPerson + (i == 0 ? remainder : 0);
                    
                    // The person who paid doesn't owe themselves
                    boolean isPaid = member.getUser().getId().equals(expense.getPaidBy().getId());
                    
                    ExpenseSplit split = ExpenseSplit.builder()
                            .expense(expense)
                            .user(member.getUser())
                            .amount(splitAmount)
                            .isPaid(isPaid)
                            .paidAt(isPaid ? LocalDateTime.now() : null)
                            .build();
                    splitRepository.save(split);
                }
                break;
                
            case PERCENTAGE:
                for (HouseholdMember member : activeMembers) {
                    int splitAmount = (expense.getAmount() * member.getRentSharePercentage()) / 100;
                    boolean isPaid = member.getUser().getId().equals(expense.getPaidBy().getId());
                    
                    ExpenseSplit split = ExpenseSplit.builder()
                            .expense(expense)
                            .user(member.getUser())
                            .amount(splitAmount)
                            .isPaid(isPaid)
                            .paidAt(isPaid ? LocalDateTime.now() : null)
                            .build();
                    splitRepository.save(split);
                }
                break;
                
            case CUSTOM:
                if (request.getCustomSplits() == null || request.getCustomSplits().isEmpty()) {
                    throw new BadRequestException("Custom splits required for CUSTOM split type");
                }
                
                for (Map.Entry<UUID, Integer> entry : request.getCustomSplits().entrySet()) {
                    User user = userRepository.findById(entry.getKey())
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + entry.getKey()));
                    boolean isPaid = entry.getKey().equals(expense.getPaidBy().getId());
                    
                    ExpenseSplit split = ExpenseSplit.builder()
                            .expense(expense)
                            .user(user)
                            .amount(entry.getValue())
                            .isPaid(isPaid)
                            .paidAt(isPaid ? LocalDateTime.now() : null)
                            .build();
                    splitRepository.save(split);
                }
                break;
                
            default:
                throw new BadRequestException("Invalid split type");
        }
    }
    
    public Page<ExpenseResponse> getHouseholdExpenses(UUID householdId, UUID userId, Pageable pageable) {
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        return expenseRepository.findByHouseholdIdOrderByExpenseDateDesc(householdId, pageable)
                .map(this::buildExpenseResponse);
    }
    
    public void markSplitAsPaid(UUID splitId, UUID userId) {
        ExpenseSplit split = splitRepository.findById(splitId)
                .orElseThrow(() -> new ResourceNotFoundException("Split not found"));
        
        // Verify user is the one who owes or the one who paid
        if (!split.getUser().getId().equals(userId) && 
            !split.getExpense().getPaidBy().getId().equals(userId)) {
            throw new BadRequestException("You can only mark your own splits as paid");
        }
        
        split.markAsPaid();
        splitRepository.save(split);
    }
    
    public ExpenseResponse recordSettlement(UUID householdId, UUID userId, SettlementRequest request) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found"));
        
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        User fromUser = userRepository.findById(userId).get();
        User toUser = userRepository.findById(request.getToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));
        
        ExpenseSettlement settlement = ExpenseSettlement.builder()
                .household(household)
                .fromUser(fromUser)
                .toUser(toUser)
                .amount(request.getAmount())
                .note(request.getNote())
                .settlementMethod(request.getSettlementMethod())
                .build();
        settlementRepository.save(settlement);
        
        // Mark splits as paid up to the settlement amount
        List<ExpenseSplit> unpaidSplits = splitRepository.findUnpaidByHouseholdAndUser(householdId, userId);
        int remainingSettlement = request.getAmount();
        
        for (ExpenseSplit split : unpaidSplits) {
            if (remainingSettlement <= 0) break;
            if (split.getExpense().getPaidBy().getId().equals(request.getToUserId())) {
                if (split.getAmount() <= remainingSettlement) {
                    split.markAsPaid();
                    remainingSettlement -= split.getAmount();
                }
            }
        }
        splitRepository.saveAll(unpaidSplits);
        
        // Notify recipient
        notificationService.createNotification(
                request.getToUserId(),
                Notification.NotificationType.PAYMENT_RECEIVED,
                "Settlement Received",
                fromUser.getFirstName() + " sent you " + formatCurrency(request.getAmount()),
                settlement.getId(), "SETTLEMENT", "/household/" + householdId
        );
        
        return null;
    }
    
    public BalanceSummaryResponse getBalanceSummary(UUID householdId, UUID userId) {
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        List<HouseholdMember> members = memberRepository.findActiveByHouseholdId(householdId);
        
        Long totalPaidByMe = splitRepository.getTotalPaidByUserInHousehold(householdId, userId);
        Long totalOwedByMe = splitRepository.getTotalOwedInHousehold(householdId, userId);
        
        // Calculate what others owe me
        Long totalOwedToMe = 0L;
        List<BalanceSummaryResponse.MemberBalance> memberBalances = new ArrayList<>();
        
        for (HouseholdMember member : members) {
            if (!member.getUser().getId().equals(userId)) {
                Long owedToMe = calculateOwedBetweenUsers(householdId, member.getUser().getId(), userId);
                Long iOwe = calculateOwedBetweenUsers(householdId, userId, member.getUser().getId());
                Long netBalance = (owedToMe != null ? owedToMe : 0) - (iOwe != null ? iOwe : 0);
                
                if (netBalance > 0) totalOwedToMe += netBalance;
                
                Profile profile = profileRepository.findByUserId(member.getUser().getId()).orElse(null);
                memberBalances.add(BalanceSummaryResponse.MemberBalance.builder()
                        .userId(member.getUser().getId())
                        .userName(member.getUser().getFirstName() + " " + member.getUser().getLastName())
                        .userPhoto(profile != null ? profile.getProfilePhotoUrl() : null)
                        .amountOwed(netBalance)
                        .build());
            }
        }
        
        return BalanceSummaryResponse.builder()
                .householdId(householdId)
                .userId(userId)
                .totalPaidByMe(totalPaidByMe != null ? totalPaidByMe : 0)
                .totalOwedByMe(totalOwedByMe != null ? totalOwedByMe : 0)
                .totalOwedToMe(totalOwedToMe)
                .netBalance(totalOwedToMe - (totalOwedByMe != null ? totalOwedByMe : 0))
                .memberBalances(memberBalances)
                .build();
    }
    
    private Long calculateOwedBetweenUsers(UUID householdId, UUID owerId, UUID owedToId) {
        // Calculate how much owerId owes to owedToId
        // This is the sum of unpaid splits where owerId is the user and owedToId paid the expense
        List<ExpenseSplit> splits = splitRepository.findUnpaidByHouseholdAndUser(householdId, owerId);
        return splits.stream()
                .filter(s -> s.getExpense().getPaidBy().getId().equals(owedToId))
                .mapToLong(ExpenseSplit::getAmount)
                .sum();
    }
    
    // ==================== HOUSE RULES ====================
    
    public HouseRulesResponse getOrCreateHouseRules(UUID householdId, UUID userId) {
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        Household household = householdRepository.findById(householdId).get();
        
        HouseRules rules = rulesRepository.findByHouseholdId(householdId)
                .orElseGet(() -> {
                    HouseRules newRules = HouseRules.builder()
                            .household(household)
                            .build();
                    return rulesRepository.save(newRules);
                });
        
        return buildHouseRulesResponse(rules, userId);
    }
    
    public HouseRulesResponse updateHouseRules(UUID householdId, UUID userId, HouseRulesRequest request) {
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        User user = userRepository.findById(userId).get();
        HouseRules rules = rulesRepository.findByHouseholdId(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("House rules not found"));
        
        // Update fields
        if (request.getQuietHoursEnabled() != null) rules.setQuietHoursEnabled(request.getQuietHoursEnabled());
        if (request.getQuietHoursStart() != null) rules.setQuietHoursStart(request.getQuietHoursStart());
        if (request.getQuietHoursEnd() != null) rules.setQuietHoursEnd(request.getQuietHoursEnd());
        if (request.getGuestPolicy() != null) rules.setGuestPolicy(request.getGuestPolicy());
        if (request.getCleaningSchedule() != null) rules.setCleaningSchedule(request.getCleaningSchedule());
        if (request.getSmokingAllowed() != null) rules.setSmokingAllowed(request.getSmokingAllowed());
        if (request.getSmokingArea() != null) rules.setSmokingArea(request.getSmokingArea());
        if (request.getDrinkingAllowed() != null) rules.setDrinkingAllowed(request.getDrinkingAllowed());
        if (request.getPetsAllowed() != null) rules.setPetsAllowed(request.getPetsAllowed());
        if (request.getPetRules() != null) rules.setPetRules(request.getPetRules());
        if (request.getKitchenRules() != null) rules.setKitchenRules(request.getKitchenRules());
        if (request.getBathroomRules() != null) rules.setBathroomRules(request.getBathroomRules());
        if (request.getLivingRoomRules() != null) rules.setLivingRoomRules(request.getLivingRoomRules());
        if (request.getCustomRules() != null) rules.setCustomRules(request.getCustomRules());
        
        rules.setLastUpdatedBy(user);
        rules = rulesRepository.save(rules);
        
        // Clear all agreements (rules changed)
        agreementRepository.deleteAll(rules.getAgreements());
        
        // Notify members about rule change
        List<HouseholdMember> members = memberRepository.findActiveByHouseholdId(householdId);
        for (HouseholdMember member : members) {
            if (!member.getUser().getId().equals(userId)) {
                notificationService.createNotification(
                        member.getUser().getId(),
                        Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                        "House Rules Updated",
                        user.getFirstName() + " updated the house rules. Please review and agree to the new rules.",
                        rules.getId(), "HOUSE_RULES", "/household/" + householdId
                );
            }
        }
        
        return buildHouseRulesResponse(rules, userId);
    }
    
    public void agreeToRules(UUID householdId, UUID userId) {
        HouseRules rules = rulesRepository.findByHouseholdId(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("House rules not found"));
        
        // Check if already agreed
        if (agreementRepository.hasAgreed(rules.getId(), userId)) {
            throw new BadRequestException("You have already agreed to these rules");
        }
        
        User user = userRepository.findById(userId).get();
        
        RuleAgreement agreement = RuleAgreement.builder()
                .houseRules(rules)
                .user(user)
                .build();
        agreementRepository.save(agreement);
    }
    
    // ==================== BILL REMINDERS ====================
    
    public BillReminderResponse createBillReminder(UUID householdId, UUID userId, CreateBillReminderRequest request) {
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        Household household = householdRepository.findById(householdId).get();
        User creator = userRepository.findById(userId).get();
        
        BillReminder reminder = BillReminder.builder()
                .household(household)
                .createdBy(creator)
                .name(request.getName())
                .category(request.getCategory())
                .amount(request.getAmount())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : true)
                .dueDay(request.getDueDay())
                .reminderDaysBefore(request.getReminderDaysBefore() != null ? request.getReminderDaysBefore() : 3)
                .build();
        
        reminder = billReminderRepository.save(reminder);
        return buildBillReminderResponse(reminder);
    }
    
    public List<BillReminderResponse> getHouseholdBillReminders(UUID householdId, UUID userId) {
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        return billReminderRepository.findActiveByHouseholdId(householdId).stream()
                .map(this::buildBillReminderResponse)
                .collect(Collectors.toList());
    }
    
    // ==================== TASK MANAGEMENT ====================
    
    public TaskResponse createTask(UUID householdId, UUID userId, CreateTaskRequest request) {
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        Household household = householdRepository.findById(householdId).get();
        User creator = userRepository.findById(userId).get();
        
        HouseholdTask task = HouseholdTask.builder()
                .household(household)
                .createdBy(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .assignmentType(request.getAssignmentType() != null ? request.getAssignmentType() : HouseholdTask.AssignmentType.VOLUNTARY)
                .dueDate(request.getDueDate())
                .dueTime(request.getDueTime())
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .recurrencePattern(request.getRecurrencePattern())
                .recurrenceDay(request.getRecurrenceDay())
                .priority(request.getPriority() != null ? request.getPriority() : HouseholdTask.TaskPriority.NORMAL)
                .build();
        
        if (request.getAssignedTo() != null) {
            User assignee = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found"));
            task.setAssignedTo(assignee);
            
            // Notify assignee
            notificationService.createNotification(
                    request.getAssignedTo(),
                    Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                    "New Task Assigned",
                    creator.getFirstName() + " assigned you a task: " + request.getTitle(),
                    task.getId(), "TASK", "/household/" + householdId
            );
        }
        
        task = taskRepository.save(task);
        return buildTaskResponse(task);
    }
    
    public List<TaskResponse> getHouseholdTasks(UUID householdId, UUID userId) {
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        return taskRepository.findPendingByHouseholdId(householdId).stream()
                .map(this::buildTaskResponse)
                .collect(Collectors.toList());
    }
    
    public List<TaskResponse> getMyTasks(UUID userId) {
        return taskRepository.findPendingByAssignedTo(userId).stream()
                .map(this::buildTaskResponse)
                .collect(Collectors.toList());
    }
    
    public TaskResponse completeTask(UUID taskId, UUID userId) {
        HouseholdTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        if (!householdRepository.isMember(task.getHousehold().getId(), userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        User user = userRepository.findById(userId).get();
        task.markCompleted(user);
        task = taskRepository.save(task);
        
        // Notify household
        notificationService.createNotification(
                task.getCreatedBy().getId(),
                Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                "Task Completed",
                user.getFirstName() + " completed the task: " + task.getTitle(),
                task.getId(), "TASK", "/household/" + task.getHousehold().getId()
        );
        
        return buildTaskResponse(task);
    }
    
    // ==================== ISSUE MANAGEMENT ====================
    
    public IssueResponse createIssue(UUID householdId, UUID userId, CreateIssueRequest request) {
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        Household household = householdRepository.findById(householdId).get();
        User reporter = userRepository.findById(userId).get();
        
        HouseholdIssue issue = HouseholdIssue.builder()
                .household(household)
                .reportedBy(reporter)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .severity(request.getSeverity() != null ? request.getSeverity() : HouseholdIssue.IssueSeverity.MEDIUM)
                .isAnonymous(request.getIsAnonymous() != null ? request.getIsAnonymous() : false)
                .build();
        
        if (request.getReportedAgainst() != null) {
            User against = userRepository.findById(request.getReportedAgainst())
                    .orElseThrow(() -> new ResourceNotFoundException("Reported user not found"));
            issue.setReportedAgainst(against);
        }
        
        issue = issueRepository.save(issue);
        
        // Notify all members
        List<HouseholdMember> members = memberRepository.findActiveByHouseholdId(householdId);
        for (HouseholdMember member : members) {
            if (!member.getUser().getId().equals(userId)) {
                notificationService.createNotification(
                        member.getUser().getId(),
                        Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                        "New Household Issue",
                        "A new issue has been reported: " + request.getTitle(),
                        issue.getId(), "ISSUE", "/household/" + householdId
                );
            }
        }
        
        return buildIssueResponse(issue, userId);
    }
    
    public List<IssueResponse> getHouseholdIssues(UUID householdId, UUID userId) {
        if (!householdRepository.isMember(householdId, userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        return issueRepository.findOpenByHouseholdId(householdId).stream()
                .map(i -> buildIssueResponse(i, userId))
                .collect(Collectors.toList());
    }
    
    public IssueResponse addComment(UUID issueId, UUID userId, String content, boolean isResolutionProposal) {
        HouseholdIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));
        
        if (!householdRepository.isMember(issue.getHousehold().getId(), userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        User user = userRepository.findById(userId).get();
        
        IssueComment comment = IssueComment.builder()
                .issue(issue)
                .user(user)
                .content(content)
                .isResolutionProposal(isResolutionProposal)
                .build();
        commentRepository.save(comment);
        
        // Update issue status if first response
        if (issue.getStatus() == HouseholdIssue.IssueStatus.OPEN) {
            issue.setStatus(HouseholdIssue.IssueStatus.IN_DISCUSSION);
            issueRepository.save(issue);
        }
        
        return buildIssueResponse(issue, userId);
    }
    
    public IssueResponse resolveIssue(UUID issueId, UUID userId, String resolution) {
        HouseholdIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));
        
        if (!householdRepository.isMember(issue.getHousehold().getId(), userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        User resolver = userRepository.findById(userId).get();
        issue.resolve(resolver, resolution);
        issue = issueRepository.save(issue);
        
        // Notify reporter
        if (!issue.getReportedBy().getId().equals(userId)) {
            notificationService.createNotification(
                    issue.getReportedBy().getId(),
                    Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Issue Resolved",
                    "Your issue '" + issue.getTitle() + "' has been resolved.",
                    issue.getId(), "ISSUE", "/household/" + issue.getHousehold().getId()
            );
        }
        
        return buildIssueResponse(issue, userId);
    }
    
    public void escalateIssue(UUID issueId, UUID userId, String escalateTo) {
        HouseholdIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found"));
        
        if (!householdRepository.isMember(issue.getHousehold().getId(), userId)) {
            throw new BadRequestException("You are not a member of this household");
        }
        
        issue.escalate(escalateTo);
        issueRepository.save(issue);
        
        // TODO: Notify platform support or landlord based on escalateTo
    }
    
    // ==================== HELPER METHODS ====================
    
    private HouseholdResponse buildHouseholdResponse(Household household, UUID userId) {
        List<HouseholdMember> members = memberRepository.findActiveByHouseholdId(household.getId());
        
        // Get expense stats
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);
        Long totalExpenses = expenseRepository.getTotalExpensesForPeriod(household.getId(), startOfMonth, endOfMonth);
        
        // Get balance info
        Long myOwed = splitRepository.getTotalOwedInHousehold(household.getId(), userId);
        Long owedToMe = 0L;
        for (HouseholdMember member : members) {
            if (!member.getUser().getId().equals(userId)) {
                Long owed = calculateOwedBetweenUsers(household.getId(), member.getUser().getId(), userId);
                if (owed != null) owedToMe += owed;
            }
        }
        
        // Get task and issue counts
        long openTasks = taskRepository.findPendingByHouseholdId(household.getId()).size();
        long openIssues = issueRepository.countOpenIssues(household.getId());
        
        List<HouseholdResponse.MemberResponse> memberResponses = members.stream()
                .map(m -> {
                    Profile profile = profileRepository.findByUserId(m.getUser().getId()).orElse(null);
                    return HouseholdResponse.MemberResponse.builder()
                            .id(m.getId())
                            .userId(m.getUser().getId())
                            .firstName(m.getUser().getFirstName())
                            .lastName(m.getUser().getLastName())
                            .email(m.getUser().getEmail())
                            .photoUrl(profile != null ? profile.getProfilePhotoUrl() : null)
                            .role(m.getRole().name())
                            .rentSharePercentage(m.getRentSharePercentage())
                            .joinedAt(m.getJoinedAt())
                            .inviteStatus(m.getInviteStatus().name())
                            .build();
                })
                .collect(Collectors.toList());
        
        return HouseholdResponse.builder()
                .id(household.getId())
                .name(household.getName())
                .description(household.getDescription())
                .status(household.getStatus())
                .leaseId(household.getLease() != null ? household.getLease().getId() : null)
                .listingId(household.getListing() != null ? household.getListing().getId() : null)
                .listingTitle(household.getListing() != null ? household.getListing().getTitle() : null)
                .listingAddress(household.getListing() != null ? household.getListing().getAddress() : null)
                .members(memberResponses)
                .createdAt(household.getCreatedAt())
                .totalExpensesThisMonth(totalExpenses != null ? totalExpenses : 0)
                .myBalanceOwed(myOwed != null ? myOwed : 0)
                .myBalanceOwedToMe(owedToMe)
                .openTasksCount((int) openTasks)
                .openIssuesCount((int) openIssues)
                .build();
    }
    
    private ExpenseResponse buildExpenseResponse(HouseholdExpense expense) {
        Profile paidByProfile = profileRepository.findByUserId(expense.getPaidBy().getId()).orElse(null);
        
        List<ExpenseResponse.SplitResponse> splits = expense.getSplits().stream()
                .map(s -> {
                    Profile profile = profileRepository.findByUserId(s.getUser().getId()).orElse(null);
                    return ExpenseResponse.SplitResponse.builder()
                            .id(s.getId())
                            .userId(s.getUser().getId())
                            .userName(s.getUser().getFirstName() + " " + s.getUser().getLastName())
                            .userPhoto(profile != null ? profile.getProfilePhotoUrl() : null)
                            .amount(s.getAmount())
                            .isPaid(s.getIsPaid())
                            .paidAt(s.getPaidAt())
                            .build();
                })
                .collect(Collectors.toList());
        
        // If splits weren't loaded, fetch them
        if (splits.isEmpty()) {
            splits = splitRepository.findByExpenseId(expense.getId()).stream()
                    .map(s -> {
                        Profile profile = profileRepository.findByUserId(s.getUser().getId()).orElse(null);
                        return ExpenseResponse.SplitResponse.builder()
                                .id(s.getId())
                                .userId(s.getUser().getId())
                                .userName(s.getUser().getFirstName() + " " + s.getUser().getLastName())
                                .userPhoto(profile != null ? profile.getProfilePhotoUrl() : null)
                                .amount(s.getAmount())
                                .isPaid(s.getIsPaid())
                                .paidAt(s.getPaidAt())
                                .build();
                    })
                    .collect(Collectors.toList());
        }
        
        return ExpenseResponse.builder()
                .id(expense.getId())
                .householdId(expense.getHousehold().getId())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .receiptUrl(expense.getReceiptUrl())
                .expenseDate(expense.getExpenseDate())
                .splitType(expense.getSplitType())
                .isRecurring(expense.getIsRecurring())
                .recurringDay(expense.getRecurringDay())
                .createdAt(expense.getCreatedAt())
                .paidById(expense.getPaidBy().getId())
                .paidByName(expense.getPaidBy().getFirstName() + " " + expense.getPaidBy().getLastName())
                .paidByPhoto(paidByProfile != null ? paidByProfile.getProfilePhotoUrl() : null)
                .splits(splits)
                .build();
    }
    
    private HouseRulesResponse buildHouseRulesResponse(HouseRules rules, UUID currentUserId) {
        List<HouseholdMember> members = memberRepository.findActiveByHouseholdId(rules.getHousehold().getId());
        
        List<HouseRulesResponse.AgreementStatus> agreements = members.stream()
                .map(m -> {
                    RuleAgreement agreement = agreementRepository.findByHouseRulesIdAndUserId(rules.getId(), m.getUser().getId())
                            .orElse(null);
                    Profile profile = profileRepository.findByUserId(m.getUser().getId()).orElse(null);
                    return HouseRulesResponse.AgreementStatus.builder()
                            .userId(m.getUser().getId())
                            .userName(m.getUser().getFirstName() + " " + m.getUser().getLastName())
                            .userPhoto(profile != null ? profile.getProfilePhotoUrl() : null)
                            .hasAgreed(agreement != null)
                            .agreedAt(agreement != null ? agreement.getAgreedAt() : null)
                            .build();
                })
                .collect(Collectors.toList());
        
        boolean allAgreed = agreements.stream().allMatch(HouseRulesResponse.AgreementStatus::getHasAgreed);
        boolean currentUserAgreed = agreementRepository.hasAgreed(rules.getId(), currentUserId);
        
        return HouseRulesResponse.builder()
                .id(rules.getId())
                .householdId(rules.getHousehold().getId())
                .quietHoursEnabled(rules.getQuietHoursEnabled())
                .quietHoursStart(rules.getQuietHoursStart())
                .quietHoursEnd(rules.getQuietHoursEnd())
                .guestPolicy(rules.getGuestPolicy())
                .cleaningSchedule(rules.getCleaningSchedule())
                .smokingAllowed(rules.getSmokingAllowed())
                .smokingArea(rules.getSmokingArea())
                .drinkingAllowed(rules.getDrinkingAllowed())
                .petsAllowed(rules.getPetsAllowed())
                .petRules(rules.getPetRules())
                .kitchenRules(rules.getKitchenRules())
                .bathroomRules(rules.getBathroomRules())
                .livingRoomRules(rules.getLivingRoomRules())
                .customRules(rules.getCustomRules())
                .agreements(agreements)
                .allMembersAgreed(allAgreed)
                .currentUserAgreed(currentUserAgreed)
                .createdAt(rules.getCreatedAt())
                .updatedAt(rules.getUpdatedAt())
                .lastUpdatedByName(rules.getLastUpdatedBy() != null ? 
                        rules.getLastUpdatedBy().getFirstName() + " " + rules.getLastUpdatedBy().getLastName() : null)
                .build();
    }
    
    private TaskResponse buildTaskResponse(HouseholdTask task) {
        boolean isOverdue = task.getDueDate() != null && 
                task.getDueDate().isBefore(LocalDate.now()) && 
                task.getStatus() != HouseholdTask.TaskStatus.COMPLETED;
        
        return TaskResponse.builder()
                .id(task.getId())
                .householdId(task.getHousehold().getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .category(task.getCategory())
                .assignmentType(task.getAssignmentType())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .dueTime(task.getDueTime())
                .isOverdue(isOverdue)
                .isRecurring(task.getIsRecurring())
                .recurrencePattern(task.getRecurrencePattern())
                .recurrenceDay(task.getRecurrenceDay())
                .createdById(task.getCreatedBy().getId())
                .createdByName(task.getCreatedBy().getFirstName() + " " + task.getCreatedBy().getLastName())
                .assignedToId(task.getAssignedTo() != null ? task.getAssignedTo().getId() : null)
                .assignedToName(task.getAssignedTo() != null ? 
                        task.getAssignedTo().getFirstName() + " " + task.getAssignedTo().getLastName() : null)
                .assignedToPhoto(task.getAssignedTo() != null ? 
                        profileRepository.findByUserId(task.getAssignedTo().getId())
                                .map(Profile::getProfilePhotoUrl).orElse(null) : null)
                .completedById(task.getCompletedBy() != null ? task.getCompletedBy().getId() : null)
                .completedByName(task.getCompletedBy() != null ? 
                        task.getCompletedBy().getFirstName() + " " + task.getCompletedBy().getLastName() : null)
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
    
    private IssueResponse buildIssueResponse(HouseholdIssue issue, UUID currentUserId) {
        List<IssueResponse.CommentResponse> comments = commentRepository.findByIssueIdOrderByCreatedAtAsc(issue.getId())
                .stream()
                .map(c -> {
                    Profile profile = profileRepository.findByUserId(c.getUser().getId()).orElse(null);
                    return IssueResponse.CommentResponse.builder()
                            .id(c.getId())
                            .userId(c.getUser().getId())
                            .userName(c.getUser().getFirstName() + " " + c.getUser().getLastName())
                            .userPhoto(profile != null ? profile.getProfilePhotoUrl() : null)
                            .content(c.getContent())
                            .isResolutionProposal(c.getIsResolutionProposal())
                            .createdAt(c.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
        
        // Hide reporter info if anonymous (unless current user is the reporter)
        boolean showReporter = !issue.getIsAnonymous() || issue.getReportedBy().getId().equals(currentUserId);
        
        return IssueResponse.builder()
                .id(issue.getId())
                .householdId(issue.getHousehold().getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .category(issue.getCategory())
                .severity(issue.getSeverity())
                .status(issue.getStatus())
                .reportedById(showReporter ? issue.getReportedBy().getId() : null)
                .reportedByName(showReporter ? 
                        issue.getReportedBy().getFirstName() + " " + issue.getReportedBy().getLastName() : "Anonymous")
                .isAnonymous(issue.getIsAnonymous())
                .reportedAgainstId(issue.getReportedAgainst() != null ? issue.getReportedAgainst().getId() : null)
                .reportedAgainstName(issue.getReportedAgainst() != null ? 
                        issue.getReportedAgainst().getFirstName() + " " + issue.getReportedAgainst().getLastName() : null)
                .resolution(issue.getResolution())
                .resolvedAt(issue.getResolvedAt())
                .resolvedById(issue.getResolvedBy() != null ? issue.getResolvedBy().getId() : null)
                .resolvedByName(issue.getResolvedBy() != null ? 
                        issue.getResolvedBy().getFirstName() + " " + issue.getResolvedBy().getLastName() : null)
                .isEscalated(issue.getIsEscalated())
                .escalatedAt(issue.getEscalatedAt())
                .escalatedTo(issue.getEscalatedTo())
                .comments(comments)
                .commentCount(comments.size())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }
    
    private BillReminderResponse buildBillReminderResponse(BillReminder reminder) {
        int today = LocalDate.now().getDayOfMonth();
        int dueDay = reminder.getDueDay() != null ? reminder.getDueDay() : 1;
        int daysUntilDue = dueDay >= today ? dueDay - today : (LocalDate.now().lengthOfMonth() - today + dueDay);
        boolean isPastDue = dueDay < today;
        
        return BillReminderResponse.builder()
                .id(reminder.getId())
                .householdId(reminder.getHousehold().getId())
                .name(reminder.getName())
                .category(reminder.getCategory())
                .amount(reminder.getAmount())
                .isRecurring(reminder.getIsRecurring())
                .dueDay(reminder.getDueDay())
                .reminderDaysBefore(reminder.getReminderDaysBefore())
                .isActive(reminder.getIsActive())
                .daysUntilDue(daysUntilDue)
                .isPastDue(isPastDue)
                .createdById(reminder.getCreatedBy().getId())
                .createdByName(reminder.getCreatedBy().getFirstName() + " " + reminder.getCreatedBy().getLastName())
                .createdAt(reminder.getCreatedAt())
                .updatedAt(reminder.getUpdatedAt())
                .build();
    }
    
    private String formatCurrency(Integer amount) {
        return String.format("%,d XAF", amount);
    }
}
