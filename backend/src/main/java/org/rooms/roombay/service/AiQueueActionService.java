package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.response.AiChatResponse;
import org.rooms.roombay.dto.response.AiChatResponse.ActionItem;
import org.rooms.roombay.dto.response.AiChatResponse.SuggestedAction;
import org.rooms.roombay.entity.LandlordVerification;
import org.rooms.roombay.entity.Payment;
import org.rooms.roombay.entity.PropertyListing;
import org.rooms.roombay.entity.Report;
import org.rooms.roombay.entity.RoomApplication;
import org.rooms.roombay.entity.User;
import org.rooms.roombay.entity.Visit;
import org.rooms.roombay.repository.LandlordVerificationRepository;
import org.rooms.roombay.repository.PaymentRepository;
import org.rooms.roombay.repository.PropertyListingRepository;
import org.rooms.roombay.repository.ReportRepository;
import org.rooms.roombay.repository.RoomApplicationRepository;
import org.rooms.roombay.repository.VisitRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Builds "queue" answers so landlords/admins never copy ids: a role-scoped read of pending items,
 * each rendered with its own confirm-action buttons. Nothing is mutated here — clicking a button
 * calls {@link AiPrivilegedActionService} through the execute endpoint, which re-validates everything.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiQueueActionService {

    private static final int MAX_ITEMS = 6;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("d MMM, HH:mm");

    private static final Pattern APPLICATIONS = Pattern.compile(
            "(?i)\\b(pending|review|new|received|waiting|incoming|show|list|my|which)\\b[\\w\\s]*\\bapplications?\\b");
    private static final Pattern VISITS = Pattern.compile(
            "(?i)\\b(pending|requested|upcoming|review|show|list|my|which)\\b[\\w\\s]*\\b(visits?|viewings?|tours?)\\b");
    private static final Pattern LISTINGS = Pattern.compile(
            "(?i)\\b(pending|review|approve|moderation|queue|show|list|which)\\b[\\w\\s]*\\blistings?\\b");
    private static final Pattern PAYMENTS = Pattern.compile(
            "(?i)\\b(pending|submitted|verify|proof|review|show|list|which)\\b[\\w\\s]*\\bpayments?\\b"
                    + "|\\bpayment\\s+proofs?\\b");
    private static final Pattern REPORTS = Pattern.compile(
            "(?i)\\b(open|pending|review|flagged|show|list|which)\\b[\\w\\s]*\\breports?\\b");
    private static final Pattern VERIFICATIONS = Pattern.compile(
            "(?i)\\b(pending|review|approve|queue|show|list|which)\\b[\\w\\s]*\\bver(ification)?s?\\b"
                    + "|\\bverification\\s+queue\\b");

    private final RoomApplicationRepository roomApplicationRepository;
    private final VisitRepository visitRepository;
    private final PropertyListingRepository propertyListingRepository;
    private final PaymentRepository paymentRepository;
    private final ReportRepository reportRepository;
    private final LandlordVerificationRepository landlordVerificationRepository;

    /** @return a queue answer with per-item confirm buttons, or empty when the message is not a queue request. */
    public Optional<AiChatResponse> tryQueue(UUID userId, String role, String message, String threadId) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);

        if ("LANDLORD".equals(normalizedRole)) {
            if (APPLICATIONS.matcher(message).find()) {
                return Optional.of(answer("pending applications", landlordApplications(userId), threadId));
            }
            if (VISITS.matcher(message).find()) {
                return Optional.of(answer("requested visits", landlordVisits(userId), threadId));
            }
        } else if ("ADMIN".equals(normalizedRole)) {
            if (LISTINGS.matcher(message).find()) {
                return Optional.of(answer("listings awaiting review", adminListings(), threadId));
            }
            if (VERIFICATIONS.matcher(message).find()) {
                return Optional.of(answer("pending landlord verifications", adminVerifications(), threadId));
            }
            if (PAYMENTS.matcher(message).find()) {
                return Optional.of(answer("payment proofs to verify", adminPayments(), threadId));
            }
            if (REPORTS.matcher(message).find()) {
                return Optional.of(answer("open reports", adminReports(), threadId));
            }
        }
        return Optional.empty();
    }

    // ----- Landlord queues ---------------------------------------------------

    private List<ActionItem> landlordApplications(UUID landlordId) {
        List<ActionItem> items = new ArrayList<>();
        for (RoomApplication a : roomApplicationRepository
                .findByLandlordIdAndStatus(landlordId, RoomApplication.Status.PENDING, PageRequest.of(0, MAX_ITEMS))) {
            UUID id = a.getId();
            String applicant = displayName(a.getStudent());
            String listing = a.getListing() == null ? "a listing" : a.getListing().getTitle();
            String sub = a.getMoveInDate() != null ? "Move-in " + a.getMoveInDate().format(DATE) : null;
            items.add(ActionItem.builder()
                    .id(id.toString())
                    .title(applicant + " · " + listing)
                    .subtitle(sub)
                    .actions(List.of(
                            confirm("accept", id, "Accept", AiPrivilegedActionService.ACCEPT_APPLICATION, null),
                            confirm("reject", id, "Reject", AiPrivilegedActionService.REJECT_APPLICATION, null)))
                    .build());
        }
        return items;
    }

    private List<ActionItem> landlordVisits(UUID landlordId) {
        List<ActionItem> items = new ArrayList<>();
        for (Visit v : visitRepository
                .findByLandlordIdAndStatus(landlordId, Visit.Status.REQUESTED, PageRequest.of(0, MAX_ITEMS))) {
            UUID id = v.getId();
            String tenant = displayName(v.getTenant());
            String listing = v.getListing() == null ? "a listing" : v.getListing().getTitle();
            String sub = v.getRequestedDatetime() != null ? "Requested " + v.getRequestedDatetime().format(DATETIME) : null;
            items.add(ActionItem.builder()
                    .id(id.toString())
                    .title(tenant + " · " + listing)
                    .subtitle(sub)
                    .actions(List.of(
                            confirm("accept", id, "Accept", AiPrivilegedActionService.ACCEPT_VISIT, null),
                            confirm("cancel", id, "Cancel", AiPrivilegedActionService.CANCEL_VISIT, null)))
                    .build());
        }
        return items;
    }

    // ----- Admin queues ------------------------------------------------------

    private List<ActionItem> adminListings() {
        List<ActionItem> items = new ArrayList<>();
        for (PropertyListing l : propertyListingRepository.findPendingListings(PageRequest.of(0, MAX_ITEMS))) {
            UUID id = l.getId();
            String sub = (l.getCity() == null ? "" : l.getCity())
                    + (l.getLandlord() != null ? " · " + displayName(l.getLandlord()) : "");
            items.add(ActionItem.builder()
                    .id(id.toString())
                    .title(l.getTitle() == null ? "Untitled listing" : l.getTitle())
                    .subtitle(sub.isBlank() ? null : sub)
                    .actions(List.of(
                            confirm("approve", id, "Approve", AiPrivilegedActionService.APPROVE_LISTING, null),
                            confirm("reject", id, "Reject", AiPrivilegedActionService.REJECT_LISTING, null)))
                    .build());
        }
        return items;
    }

    private List<ActionItem> adminVerifications() {
        List<ActionItem> items = new ArrayList<>();
        for (LandlordVerification lv : landlordVerificationRepository
                .findAllPendingVerifications(PageRequest.of(0, MAX_ITEMS))) {
            String type = primaryPendingType(lv);
            if (type == null) {
                continue;
            }
            UUID id = lv.getId();
            Map<String, String> params = Map.of("targetId", id.toString(), "verificationType", type);
            items.add(ActionItem.builder()
                    .id(id.toString())
                    .title(displayName(lv.getUser()) + " · " + type.toLowerCase(Locale.ROOT) + " verification")
                    .subtitle("Pending review")
                    .actions(List.of(
                            confirmWith("approve", id, "Approve", AiPrivilegedActionService.APPROVE_VERIFICATION, params),
                            confirmWith("reject", id, "Reject", AiPrivilegedActionService.REJECT_VERIFICATION, params)))
                    .build());
        }
        return items;
    }

    private List<ActionItem> adminPayments() {
        List<ActionItem> items = new ArrayList<>();
        for (Payment p : paymentRepository.findPendingVerification(PageRequest.of(0, MAX_ITEMS))) {
            UUID id = p.getId();
            String amount = p.getAmount() == null ? "Payment" : String.format(Locale.ROOT, "%,d XAF", p.getAmount());
            String sub = (p.getReferenceCode() != null ? "Ref " + p.getReferenceCode() : null);
            items.add(ActionItem.builder()
                    .id(id.toString())
                    .title(amount + " · " + displayName(p.getPayer()))
                    .subtitle(sub)
                    .actions(List.of(
                            confirm("verify", id, "Verify", AiPrivilegedActionService.VERIFY_PAYMENT, null)))
                    .build());
        }
        return items;
    }

    private List<ActionItem> adminReports() {
        List<ActionItem> items = new ArrayList<>();
        for (Report r : reportRepository
                .findByStatus(Report.ReportStatus.PENDING, PageRequest.of(0, MAX_ITEMS))) {
            UUID id = r.getId();
            String reason = r.getReason() == null ? "Report" : r.getReason().name().replace('_', ' ').toLowerCase(Locale.ROOT);
            String entity = r.getReportedEntityType() == null ? "" : " · " + r.getReportedEntityType().name().toLowerCase(Locale.ROOT);
            items.add(ActionItem.builder()
                    .id(id.toString())
                    .title(capitalize(reason) + entity)
                    .subtitle(truncate(r.getDescription(), 80))
                    .actions(List.of(
                            confirm("resolve", id, "Resolve", AiPrivilegedActionService.RESOLVE_REPORT, null)))
                    .build());
        }
        return items;
    }

    // ----- helpers -----------------------------------------------------------

    private AiChatResponse answer(String label, List<ActionItem> items, String threadId) {
        String text = items.isEmpty()
                ? "You have no " + label + " right now."
                : "You have " + items.size() + (items.size() == MAX_ITEMS ? "+" : "") + " " + label
                        + ". Use the buttons on each to confirm an action — no ids to copy.";
        return AiChatResponse.builder()
                .answer(text)
                .threadId(threadId)
                .citations(List.of())
                .suggestedActions(List.of())
                .listingResults(List.of())
                .ragGrounded(false)
                .toolExecutions(List.of())
                .actionItems(items)
                .build();
    }

    private SuggestedAction confirm(String verb, UUID id, String label, String tool, Map<String, String> extra) {
        return confirmWith(verb, id, label, tool, Map.of("targetId", id.toString()));
    }

    private SuggestedAction confirmWith(String verb, UUID id, String label, String tool, Map<String, String> params) {
        return SuggestedAction.builder()
                .id(verb + "-" + id)
                .label(label)
                .type("CONFIRM_ACTION")
                .tool(tool)
                .actionParams(params)
                .build();
    }

    private static String primaryPendingType(LandlordVerification lv) {
        if (lv.getIdentityStatus() == LandlordVerification.VerificationStatus.PENDING) {
            return "IDENTITY";
        }
        if (lv.getBusinessStatus() == LandlordVerification.VerificationStatus.PENDING) {
            return "BUSINESS";
        }
        if (lv.getPropertyStatus() == LandlordVerification.VerificationStatus.PENDING) {
            return "PROPERTY";
        }
        return null;
    }

    private static String displayName(User u) {
        if (u == null) {
            return "Unknown";
        }
        String name = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        if (!name.isEmpty()) {
            return name;
        }
        return u.getEmail() == null ? "Unknown" : u.getEmail();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String truncate(String s, int max) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.strip();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }
}
