package org.rooms.roombay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a member of a household with their role and rent share.
 */
@Entity
@Table(name = "household_members", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"household_id", "user_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HouseholdMember {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;
    
    @Column(name = "rent_share_percentage")
    @Builder.Default
    private Integer rentSharePercentage = 50;
    
    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();
    
    @Column(name = "left_at")
    private LocalDateTime leftAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "invite_status")
    @Builder.Default
    private InviteStatus inviteStatus = InviteStatus.ACCEPTED;
    
    public enum MemberRole {
        ADMIN,  // Can manage household settings, invite members
        MEMBER  // Regular member
    }
    
    public enum InviteStatus {
        PENDING,
        ACCEPTED,
        DECLINED
    }
    
    public boolean isActive() {
        return leftAt == null && inviteStatus == InviteStatus.ACCEPTED;
    }
}
