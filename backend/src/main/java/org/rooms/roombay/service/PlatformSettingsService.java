package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rooms.roombay.dto.request.PlatformSettingsUpdateRequest;
import org.rooms.roombay.dto.response.PlatformSettingsResponse;
import org.rooms.roombay.entity.PlatformSettings;
import org.rooms.roombay.repository.PlatformSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlatformSettingsService {

    private final PlatformSettingsRepository platformSettingsRepository;

    @Transactional(readOnly = true)
    public PlatformSettingsResponse getSettings() {
        PlatformSettings settings = getOrCreateSettings();
        return mapToResponse(settings);
    }

    public PlatformSettingsResponse updateSettings(PlatformSettingsUpdateRequest request) {
        PlatformSettings settings = getOrCreateSettings();

        settings.setMaintenanceMode(request.getMaintenanceMode());
        settings.setAllowNewRegistrations(request.getAllowNewRegistrations());
        settings.setRequireEmailVerification(request.getRequireEmailVerification());
        settings.setRequireTenantVerification(request.getRequireTenantVerification());
        settings.setAutoApproveListings(request.getAutoApproveListings());
        settings.setMaxListingsPerLandlord(request.getMaxListingsPerLandlord());
        settings.setPlatformFeePercent(request.getPlatformFeePercent());
        settings.setSupportEmail(request.getSupportEmail());
        settings.setNotifyOnNewListing(request.getNotifyOnNewListing());
        settings.setNotifyOnNewVerification(request.getNotifyOnNewVerification());

        PlatformSettings saved = platformSettingsRepository.save(settings);
        log.info("Platform settings updated by admin");
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PlatformSettings getRawSettings() {
        return getOrCreateSettings();
    }

    private PlatformSettings getOrCreateSettings() {
        return platformSettingsRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> {
                    PlatformSettings defaults = PlatformSettings.builder().build();
                    return platformSettingsRepository.save(defaults);
                });
    }

    private PlatformSettingsResponse mapToResponse(PlatformSettings settings) {
        return PlatformSettingsResponse.builder()
                .maintenanceMode(settings.getMaintenanceMode())
                .allowNewRegistrations(settings.getAllowNewRegistrations())
                .requireEmailVerification(settings.getRequireEmailVerification())
                .requireTenantVerification(settings.getRequireTenantVerification())
                .autoApproveListings(settings.getAutoApproveListings())
                .maxListingsPerLandlord(settings.getMaxListingsPerLandlord())
                .platformFeePercent(settings.getPlatformFeePercent())
                .supportEmail(settings.getSupportEmail())
                .notifyOnNewListing(settings.getNotifyOnNewListing())
                .notifyOnNewVerification(settings.getNotifyOnNewVerification())
                .build();
    }
}
