package org.rooms.roombay.entity;

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
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "platform_settings")
@Builder
public class PlatformSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "maintenance_mode", nullable = false)
    @Builder.Default
    private Boolean maintenanceMode = false;

    @Column(name = "allow_new_registrations", nullable = false)
    @Builder.Default
    private Boolean allowNewRegistrations = true;

    @Column(name = "require_email_verification", nullable = false)
    @Builder.Default
    private Boolean requireEmailVerification = true;

    @Column(name = "require_tenant_verification", nullable = false)
    @Builder.Default
    private Boolean requireTenantVerification = true;

    @Column(name = "auto_approve_listings", nullable = false)
    @Builder.Default
    private Boolean autoApproveListings = false;

    @Column(name = "max_listings_per_landlord", nullable = false)
    @Builder.Default
    private Integer maxListingsPerLandlord = 10;

    @Column(name = "platform_fee_percent", nullable = false)
    @Builder.Default
    private Integer platformFeePercent = 5;

    @Column(name = "support_email", nullable = false)
    @Builder.Default
    private String supportEmail = "support@roombay.cm";

    @Column(name = "notify_on_new_listing", nullable = false)
    @Builder.Default
    private Boolean notifyOnNewListing = true;

    @Column(name = "notify_on_new_verification", nullable = false)
    @Builder.Default
    private Boolean notifyOnNewVerification = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
